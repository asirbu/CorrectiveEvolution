package eu.fbk.soa.evolution;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import eu.fbk.soa.evolution.Correction.Type;
import eu.fbk.soa.process.GoalWithPriorities;
import eu.fbk.soa.process.ProcessModel;
import eu.fbk.soa.process.domain.DomainObject;
import eu.fbk.soa.util.ConfigUtils;
import eu.fbk.soa.util.IOUtils;
import eu.fbk.soa.xml.XMLLoader;
import eu.fbk.soa.xml.XMLParsingException;


public class CarLogisticsScenarioTest {
	
	private static Logger logger = Logger.getLogger(CarLogisticsScenarioTest.class);
	
	private static String scenario = "carLogistics";
	
	private static String outputPath;
	
	public CarLogisticsScenarioTest() {
		String outDir = "output";
		try {
			outDir = ConfigUtils.getProperty("outputDir");
		} catch (IOException e) {
			e.printStackTrace();
		}
		String path = outDir + File.separator + scenario;
		File outFile = new File(path);
		outFile.mkdirs();
		
		outputPath = path + File.separator;
	}
	
	public Set<ProcessModel> testCorrectiveEvolution(String outputPath, Type corrType1, Type corrType2) throws XMLParsingException {
		Set<DomainObject> objects = XMLLoader.loadAllDomainObjects(scenario);
	
		Map<String, ProcessModel> models = XMLLoader.loadAllProcessModels(scenario, objects);
			
		GoalWithPriorities goal = XMLLoader.loadGoal(scenario, "Goal.xml", objects);
		
		List<Correction> corrections = 
			InputConstructor.constructLogisticsCorrections(models, corrType1, corrType2);
		
		CorrectiveEvolution evolution = new CorrectiveEvolution(outputPath);
		Set<ProcessModel> newModels = 
			evolution.evolveProcessModel(models.get("CarProcess"), goal, corrections);
		
		return newModels;
	}
	
	
	public static void main(String[] args) throws Exception {
		PropertyConfigurator.configure("log4j.properties");
        
		CarLogisticsScenarioTest test = new CarLogisticsScenarioTest();

		Type[] corrType1 = {Type.STRICT, Type.STRICT, Type.RELAXED, Type.RELAXED};
		Type[] corrType2 = {Type.STRICT, Type.RELAXED, Type.STRICT, Type.RELAXED};

		for (int i = 0; i < 4; i++) {
			String pbName = corrType1[i] + "_" + corrType2[i];
			
			logger.info("Solving the " + pbName + " problem");
			
			String completeOutputPath = outputPath + "output_" + i + "_" + pbName + File.separator;
		
			Set<ProcessModel> newModels = 
				test.testCorrectiveEvolution(completeOutputPath, corrType1[i], corrType2[i]);
			
			int j = 1;
			for (ProcessModel model : newModels) {
				String fileName = completeOutputPath + "result_" + i + "." + j + "_" + pbName;
				String dotFilePath =  fileName + ".dot";
				String pictFilePath = fileName + ".png";

				IOUtils.exportProcessModelToDot(model, dotFilePath);	
				IOUtils.createImage(dotFilePath, pictFilePath);
				j++;
			}
			
			String toPrint = "Problem " + pbName + ": found " + newModels.size() + " model";
			if (newModels.size() > 1) {
				toPrint += "s";
			}
			logger.info(toPrint + "\n" +
					"*******************************************************************\n");

		}
	}	

}
