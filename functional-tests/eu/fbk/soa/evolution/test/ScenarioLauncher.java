package eu.fbk.soa.evolution.test;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import eu.fbk.soa.util.ConfigUtils;

public class ScenarioLauncher {
	
	private static Logger logger = Logger.getLogger(ScenarioLauncher.class);
	
	private static String getOutputPath() {
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
		return outDir + File.separator;
	}
	
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		
		PropertyConfigurator.configure("log4j.properties");
		String outputPath = getOutputPath();
		String scenario = "financial";
		String type = "relaxed";
		
		for (String str : args) {
			if (str.startsWith("-help")) {
				printHelp();
				return;
			}
			if (str.startsWith("-scenario")) {
				scenario = str.split("=")[1];
			}
			if (str.startsWith("-type")) {
				type = str.split("=")[1];
			}
		}
		
		logger.info("Running " + scenario + " scenario...");
		
		if (scenario.equals("basic")) {
			BasicCorrectiveEvolutionScenario basicTest = 
					new BasicCorrectiveEvolutionScenario();
			basicTest.testCorrectiveEvolution(outputPath);
		}
		
		if (scenario.equals("carLogistics")) {
			CarLogisticsScenario carLogisticsTest = 
					new CarLogisticsScenario();
			carLogisticsTest.testCorrectiveEvolution(outputPath);
		}
		
		if (scenario.equals("financial")) {
			FinancialScenario finTest = new FinancialScenario(outputPath);
			if (type.equals("relaxed")) {
				finTest.testFinancialRelaxed();
			} else {
				if (type.equals("strict")) {
					finTest.testFinancialStrict();
				} else {
					logger.error("Could not recognize type.. aborting");
				}
			}
		}
	}
	
	private static void printHelp() {
		System.out.println("Options:");
		System.out.println("-scenario\t Select scenario: " +
				"basic, carLogistics, financial (default)");
		System.out.println("-type\t\t For the financial scenario, this option selects \n\t\t" +
				" the type of corrections: strict or relaxed (default)");
		System.out.println("-help\t\t Prints current message");
	}

}
