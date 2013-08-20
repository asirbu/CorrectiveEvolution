package eu.fbk.soa.xml.test;

import java.io.File;
import java.util.Set;

import eu.fbk.soa.process.Activity;
import eu.fbk.soa.process.ProcessModel;
import eu.fbk.soa.process.domain.DomainObject;
import eu.fbk.soa.util.IOUtils;
import eu.fbk.soa.xml.XMLLoader;
import eu.fbk.soa.xml.XMLParsingException;


public class XMLLoaderTest {
	
	private static String scenario = "financial";
	
	
	public static void main(String[] args) throws XMLParsingException {

		Set<DomainObject> objects = XMLLoader.loadAllDomainObjects(scenario);
		Set<Activity> acts = XMLLoader.loadActivitySet(scenario, objects);
		for (Activity act : acts) {
			System.out.println(act.getName());
		}
		
		
		ProcessModel model = XMLLoader.loadProcessModel(scenario, 
				"financial-with-decline.xml", objects, acts);
		
		String fileName = scenario + File.separator + "processes" + File.separator +
				"model-" + model.getName();
		String dotFilePath =  fileName + ".dot";
		String pictFilePath = fileName + ".png";

		IOUtils.exportProcessModelToDot(model, dotFilePath);
		IOUtils.createImage(dotFilePath, pictFilePath);

	}
}
