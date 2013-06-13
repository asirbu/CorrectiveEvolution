package eu.fbk.soa.xml;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import eu.fbk.soa.process.Activity;
import eu.fbk.soa.process.DefaultProcessModel;
import eu.fbk.soa.process.GoalWithPriorities;
import eu.fbk.soa.process.ProcessModel;
import eu.fbk.soa.process.domain.DomainObject;


public class XMLLoader {
	
	static Logger logger = Logger.getLogger(XMLLoader.class);
	
	private static String objectDir = "domainObjects";
	
	private static String processDir = "processes";
	
	private static String activitiesFileName = "activities";
	
//	private static String activitiesFileName = "activities-for-extended";
		
	
	public static Set<DomainObject> loadAllDomainObjects(String scenario) {
		Set<DomainObject> objects = new HashSet<DomainObject>();
		File objDir = new File(scenario + File.separator + objectDir);
		if (!objDir.exists()) {
			logger.warn("No domain objects loaded");
			return objects;
		}
		File[] objFiles = objDir.listFiles();

		for (File file : objFiles) {
			if (file.isFile()) {
				DomainObject obj = XMLAdapter.unmarshalDomainObject(file);
				objects.add(obj);
			}
		}
		return objects;
	}
	
	public static DomainObject loadDomainObject(String scenario, String objFileName) {
		File file = new File(scenario + File.separator + 
				objectDir + File.separator + objFileName);
		DomainObject obj = XMLAdapter.unmarshalDomainObject(file);
		return obj;
	}
	
	public static GoalWithPriorities loadGoal(String scenario, 
			String goalFileName, Set<DomainObject> objects) {
		
		File file = new File(scenario + File.separator + 
				processDir + File.separator + goalFileName);
		
		GoalWithPriorities newGoal = XMLAdapter.unmarshalGoal(file);
		newGoal.updateObjectReferences(objects);
		return newGoal;
	}
	
	
	public static DefaultProcessModel loadProcessModel(String scenario, 
			String processFileName, Set<DomainObject> objects) throws XMLParsingException {
		
		File file = new File(scenario + File.separator + 
				processDir + File.separator + processFileName);
		XMLProcessModel newModel = XMLAdapter.unmarshalModel(file);	
		
		File actFile = new File(scenario + File.separator + 
				processDir + File.separator + activitiesFileName);
		if (actFile.exists()) {
			Set<Activity> acts = loadActivitySet(scenario, objects);
			newModel.setActivities(acts);
		}
		
		newModel.updateReferences(objects);
		
		DefaultProcessModel processModel = new DefaultProcessModel(newModel.getName(), 
				newModel.getNodes());
		
		for (XMLProcessEdge edge : newModel.getEdges()) {
			processModel.addEdge(edge.getSource(), edge.getTarget(), edge.getCondition());
		}
		return processModel;
	}
	
	public static Set<Activity> loadActivitySet(String scenario, Set<DomainObject> objects) {
		File file = new File(scenario + File.separator + 
				processDir + File.separator + activitiesFileName + ".xml");
		
		XMLActivitySet actSet = XMLAdapter.unmarshalActivitySet(file);
		for (Activity act : actSet.getActivities()) {
			act.updateObjectReferences(objects);
		}
		return actSet.getActivities();
	}
	

	public static Map<String, ProcessModel> loadAllProcessModels(String scenario,
			Set<DomainObject> objects) throws XMLParsingException {
		Map<String, ProcessModel> models = new HashMap<String, ProcessModel>();
		
		File procDir = new File(scenario + File.separator + processDir);
		File[] procFiles = procDir.listFiles();

		for (File file : procFiles) {
			if (file.isFile()) {
				ProcessModel model = loadProcessModel(scenario, file.getName(), objects);
				models.put(model.getName(), model);
			}
		}
		return models;
	}

	public static ProcessModel loadProcessModel(String scenario, String processFileName,
			Set<DomainObject> objects, Set<Activity> acts) throws XMLParsingException {
		
		File file = new File(scenario + File.separator + 
				processDir + File.separator + processFileName);
		XMLProcessModel newModel = XMLAdapter.unmarshalModel(file);	
		
		newModel.setActivities(acts);
		newModel.updateReferences(objects);
		
		ProcessModel processModel = new DefaultProcessModel(newModel.getName(), 
				newModel.getNodes());
		
		for (XMLProcessEdge edge : newModel.getEdges()) {
			processModel.addEdge(edge.getSource(), edge.getTarget(), edge.getCondition());
		}
		return processModel;
	}
	

}
