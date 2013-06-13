package eu.fbk.soa.evolution.engine;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import eu.fbk.soa.evolution.Correction;
import eu.fbk.soa.evolution.engine.impl.ProblemToSMV;
import eu.fbk.soa.evolution.sts.STS;
import eu.fbk.soa.evolution.sts.nusmv.PathExplorer;
import eu.fbk.soa.process.ProcessModel;

public class StrictEngine extends AbstractEngine {

	static Logger logger = Logger.getLogger(StrictEngine.class);
	
	private PathExplorer explorer;
	
	public StrictEngine(ProcessModel model, List<Correction> corrections) {
		super();
		this.smvTranslator = new ProblemToSMV(model, corrections);
	}
	
	@Override
	public Set<ProcessModel> solveProblem() {
		logger.info("Solving strict corrective evolution problem");
		Set<ProcessModel> models = new HashSet<ProcessModel>();
		
		this.explorer = new PathExplorer(outputDir);
		String smvFilePath = outputDir + "evolution_problem.smv";
		
		translateProblemToSMV(smvFilePath);
		STS completeSTS = this.getCompleteSTS(smvFilePath);
		
//		handleIntermediaryFile(smvFilePath);
		
		if (completeSTS.getStates().isEmpty()) {
			return models;
		}
		
		logger.info("Minimizing STS");
		STS minSTS = this.getMinimizedSTS(completeSTS, "trace_minimized");
		
		logger.info("Translating STS back to a process model");
		ProcessModel corrModel = this.getCorrectedProcessModel(minSTS);
		models.add(corrModel);
		
		this.exportResultAsPetriNet("strict");
		
		return models;
	}
	

	private STS getCompleteSTS(String smvFilePath) {
		STS completeSTS = null;	
		try {
			completeSTS = explorer.explore(smvFilePath);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if (completeSTS != null) {
			handleIntermediarySTS(completeSTS, "complete_sts");
		}
		return completeSTS;
	}
	
}
