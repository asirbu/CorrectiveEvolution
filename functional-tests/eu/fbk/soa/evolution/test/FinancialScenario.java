package eu.fbk.soa.evolution.test;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.deckfour.xes.in.XesXmlParser;
import org.deckfour.xes.model.XLog;

import eu.fbk.soa.eventlog.CorrectionGenerator;
import eu.fbk.soa.eventlog.LogExplorer;
import eu.fbk.soa.eventlog.TraceDifference;
import eu.fbk.soa.evolution.Correction;
import eu.fbk.soa.evolution.CorrectiveEvolution;
import eu.fbk.soa.process.Activity;
import eu.fbk.soa.process.GoalWithPriorities;
import eu.fbk.soa.process.ProcessModel;
import eu.fbk.soa.process.domain.DomainObject;
import eu.fbk.soa.util.IOUtils;
import eu.fbk.soa.xml.XMLLoader;
import eu.fbk.soa.xml.XMLParsingException;


public class FinancialScenario {
	
	private static Logger logger = Logger.getLogger(FinancialScenario.class);
	
	private CorrectionGenerator corrGenerator;
		
	private ProcessModel model;
	
	private Set<Activity> allActivities;
	
	private CorrectiveEvolution evolution;
	
	private String outputPath;
	
	private String generalOutPath = "";
	
	public FinancialScenario(String outPath) {
		this.generalOutPath = outPath;
	}
	
	private void init(String scenario, String processName, String logName) throws Exception {
		this.loadModels(scenario, processName);
		
		File source = new File(scenario + File.separator + "eventLogs" + 
				File.separator + logName + ".xes");
		XLog log = (new XesXmlParser()).parse(source).get(0);
		
		LogExplorer logExplorer = new LogExplorer(model);
		logExplorer.processEventLog(log);
		
		corrGenerator = new CorrectionGenerator(model, allActivities, logExplorer);
		
		outputPath = generalOutPath + scenario + File.separator;
		
		evolution = new CorrectiveEvolution(outputPath);
//		evolution.enableExperimentMode();
	}

	private void loadModels(String scenario, String processName) throws XMLParsingException {
		Set<DomainObject> objects = XMLLoader.loadAllDomainObjects(scenario);
		
		allActivities = XMLLoader.loadActivitySet(scenario, objects);
		
		model = XMLLoader.loadProcessModel(scenario, processName + ".xml", objects, allActivities);
	}
	
	
	public void testFinancialRelaxed() throws Exception {
		init("financial", "financial", "financial_log (first month)");
		
		Map<TraceDifference, Double> diffsWithOccurences = new HashMap<TraceDifference, Double>();
		
		List<TraceDifference> diffs = corrGenerator.getDifferencesWithTraces(diffsWithOccurences);
//		List<TraceDifference> diffs = corrGenerator.getDifferencesWithoutTraces(diffsWithOccurences);
		
		List<Correction> corrections = 
				corrGenerator.generateRelevantRelaxedCorrections(diffsWithOccurences, diffs);
		logger.info("Generated " + corrections.size() + " corrections");
		
		Set<DomainObject> objects = model.getRelatedDomainObjects();
		
		GoalWithPriorities goal = XMLLoader.loadGoal("financial", "goal.xml", objects);
		
		Set<ProcessModel> newModels = 
				evolution.evolveProcessModel(model, goal, corrections);

		if (!newModels.isEmpty()) {
			ProcessModel newModel = newModels.iterator().next();
			printProcessModel(newModel, outputPath + "result-relaxed");
		}
	}
	
	public void testFinancialStrict() throws Exception {
		init("financial", "financial", "financial_log (first month)");
		
		List<Correction> corrections = corrGenerator.generateRelevantStrictCorrections();
		logger.info("Generated " + corrections.size() + " corrections");
		
		Set<ProcessModel> newModels = 
				evolution.evolveProcessModel(model, new GoalWithPriorities(), corrections);
		
		if (!newModels.isEmpty()) {
			ProcessModel newModel = newModels.iterator().next();
			printProcessModel(newModel, outputPath + "result-strict");
		}
	}

	private void printProcessModel(ProcessModel model, String fileName) {
		String dotFilePath =  fileName + ".dot";
		String pictFilePath = fileName + ".png";

		IOUtils.exportProcessModelToDot(model, dotFilePath);	
		IOUtils.createImage(dotFilePath, pictFilePath);
	}
	
	
}
