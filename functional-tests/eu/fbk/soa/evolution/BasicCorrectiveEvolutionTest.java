package eu.fbk.soa.evolution;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import eu.fbk.soa.evolution.Correction.Type;
import eu.fbk.soa.process.EaGLeGoal;
import eu.fbk.soa.process.Goal;
import eu.fbk.soa.process.ProcessModel;
import eu.fbk.soa.util.ConfigUtils;
import eu.fbk.soa.util.IOUtils;


public class BasicCorrectiveEvolutionTest {
	
	private static Logger logger = Logger.getLogger(CarLogisticsScenarioTest.class);
	
	private static String outputPath;
	
	public BasicCorrectiveEvolutionTest() {
		String outDir = "output";
		try {
			outDir = ConfigUtils.getProperty("outputDir");
		} catch (IOException e) {
			e.printStackTrace();
		}
		File outFile = new File(outDir);
		if (!outFile.exists()) {
			outFile.mkdir();
		}
		outputPath = outDir + File.separator;
	}
	
	public Set<ProcessModel> testCorrectiveEvolution(Type corrType1, Type corrType2) {
		Map<String, ProcessModel> models = InputConstructor.constructLogisticsProcessModels();
		
		List<Correction> corrections = 
			InputConstructor.constructLogisticsCorrections(models, corrType1, corrType2);
		
		Goal goal = null;
		if (corrType1.equals(Type.STRICT) && corrType2.equals(Type.STRICT)) {
			goal = new EaGLeGoal();
		} else {
			goal = InputConstructor.constructLogisticsGoal(models);
		}
		
		CorrectiveEvolution evolution = new CorrectiveEvolution(outputPath);
		Set<ProcessModel> newModels = 
			evolution.evolveProcessModel(models.get("CarProcess"), goal, corrections);
		
		logger.info("Found " + newModels.size() + " models");
		return newModels;
	}
	
	
	public static void main(String[] args) throws Exception {
		PropertyConfigurator.configure("log4j.properties");
		
		BasicCorrectiveEvolutionTest test = new BasicCorrectiveEvolutionTest();

		Type[] corrType1 = {Type.STRICT, Type.STRICT, Type.RELAXED, Type.RELAXED};
		Type[] corrType2 = {Type.STRICT, Type.RELAXED, Type.STRICT, Type.RELAXED};

		for (int i = 0; i < 4; i++) {
			Set<ProcessModel> newModels = 
				test.testCorrectiveEvolution(corrType1[i], corrType2[i]);

			int j = 1;
			for (ProcessModel model : newModels) {
				String fileName = outputPath + "result_" + i + "." + j + "_" + corrType1[i] + "_" + corrType2[i];
				String dotFilePath =  fileName + ".dot";
				String pictFilePath = fileName + ".png";

				IOUtils.exportProcessModelToDot(model, dotFilePath);	
				IOUtils.createImage(dotFilePath, pictFilePath);
				j++;
			}
		}
	}	

}
