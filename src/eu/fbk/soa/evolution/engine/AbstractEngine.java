package eu.fbk.soa.evolution.engine;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.apache.log4j.Logger;

import eu.fbk.soa.eventlog.DifferenceAnalysis;
import eu.fbk.soa.evolution.engine.impl.Condition;
import eu.fbk.soa.evolution.engine.impl.ProblemToSMV;
import eu.fbk.soa.evolution.engine.impl.STSToProcessModel;
import eu.fbk.soa.evolution.sts.Action;
import eu.fbk.soa.evolution.sts.STS;
import eu.fbk.soa.evolution.sts.impl.NusmvSTS;
import eu.fbk.soa.evolution.sts.minimizer.STSMinimizer;
import eu.fbk.soa.evolution.sts.minimizer.TraceEquivalenceMinimizer;
import eu.fbk.soa.process.Activity;
import eu.fbk.soa.process.ProcessModel;
import eu.fbk.soa.util.ConfigUtils;
import eu.fbk.soa.util.IOUtils;

public abstract class AbstractEngine 
	implements CorrectiveEvolutionEngine {
	
	protected static Logger logger = Logger.getLogger(AbstractEngine.class);
	
	protected String outputDir = "";
	
	protected ProblemToSMV smvTranslator;
	
	protected STSToProcessModel stsTranslator;

	private STSMinimizer minimizer;
	
	private boolean experimentMode = false;
	
	protected boolean intermediaryFiles = false;
	
	
	public AbstractEngine() {
		this.minimizer = new TraceEquivalenceMinimizer();
		this.stsTranslator = new STSToProcessModel();

		try {
			intermediaryFiles = Boolean.getBoolean(
					ConfigUtils.getProperty("intermediaryFiles"));
		} catch (IOException e) {
			logger.warn("Could not access configuration file, assuming no intermediary files are required");
		}
	}
	
	public void enableExperimentMode() {
		this.experimentMode = true;
	}
	
	@Override
	public void setOutputPath(String path) {
		this.outputDir  = path;
	}
	
	protected void translateProblemToSMV(String smvFilePath) {
		String translation = smvTranslator.translateProblemToSMV();
		IOUtils.writeStringToFile(translation, smvFilePath);
	}

	protected ProcessModel getCorrectedProcessModel(STS sts) {
		Map<Action, Activity> actionMap = 
			smvTranslator.getActionActivityCorrespondences();
		Map<Action, Condition> action2cond = 
			smvTranslator.getActionConditionCorrespondences();
		
		ProcessModel correctedModel = stsTranslator.sts2ProcessModel(sts, actionMap, 
				action2cond);
		
		return correctedModel;
	}

	protected void exportResultAsPetriNet(String fileName) {
		STS coarseSTS = this.stsTranslator.getSimplifiedSTS();
		
		STS simplifiedSTS = this.stsTranslator.simplifySTS(coarseSTS);
		
		STS minimized = getMinimizedSTS(simplifiedSTS, "simplified_and_min_sts");
		
		String petriNetFileName = fileName;
		if (experimentMode) {
			petriNetFileName += " (";
			if (DifferenceAnalysis.maxPercentageThreshold < 100.0) {
				petriNetFileName += DifferenceAnalysis.maxPercentageThreshold + "-";
			}
			petriNetFileName += DifferenceAnalysis.minPercentageThreshold + "% strict diffs)";
		}
		IOUtils.exportSTSAsPNML(minimized, outputDir + petriNetFileName + ".pnml");
	}
	
	
	protected STS getMinimizedSTS(STS completeSTS, String fileName) {
		logger.debug("Minimizing STS");
		File traceMin = new File(outputDir + fileName + ".dot");		
		minimizer.minimizeSTS(completeSTS, traceMin);
	
		STS resultSTS = IOUtils.readSTSFromFile(traceMin);
		logger.debug("STS after trace minimization:\n\n" + resultSTS.toDot());
		
		handleIntermediaryDotFile(fileName);
		return resultSTS;
	}
	
	 protected NusmvSTS getMinimizedNusmvSTS(String inputFilePath) {
		logger.debug("Minimizing STS");

		File traceMin = new File(outputDir + "trace_minimized.dot");	
		minimizer.minimizeSTS(new File(inputFilePath), traceMin);
		
		NusmvSTS resultSTS = IOUtils.readNusmvSTSFromFile(traceMin);
		
		handleIntermediaryDotFile("trace_minimized");
		return resultSTS;
	}
	 
	protected void handleIntermediarySTS(STS intermSTS, String fileName) {
		if (intermediaryFiles) {
			String dotFilePath = outputDir + fileName + ".dot";
			String pictFilePath = outputDir + fileName + ".png";

			IOUtils.exportSTSToDot(intermSTS, dotFilePath);
			IOUtils.createImage(dotFilePath, pictFilePath);
		}
	}
	
	protected void handleIntermediaryDotFile(String name) {
		File dotFile = new File(outputDir + name + ".dot");	
		if (intermediaryFiles) {
			IOUtils.createImage(dotFile.getPath(),  outputDir + name + ".png");
		} else {
			dotFile.deleteOnExit();
		}
	}
	
	protected void handleIntermediaryFile(String filePath) {
		File file = new File(filePath);	
		if (!intermediaryFiles) {
			file.deleteOnExit();
		}
	}
}
