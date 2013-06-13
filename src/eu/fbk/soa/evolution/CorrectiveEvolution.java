package eu.fbk.soa.evolution;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import eu.fbk.soa.evolution.engine.CorrectiveEvolutionEngine;
import eu.fbk.soa.evolution.engine.EngineFactory;
import eu.fbk.soa.process.Goal;
import eu.fbk.soa.process.ProcessModel;


public class CorrectiveEvolution {
	
	private static Logger logger = Logger.getLogger(CorrectiveEvolution.class);
	
	private String path = "";
	
	private boolean experimentMode = false;
		
	public CorrectiveEvolution(String path) {
		this.setOutputPath(path);		
	}
	
	public void setOutputPath(String path) {
		this.path = path;
		File outDir = new File(path);
		if (!outDir.exists()) {
			outDir.mkdirs();
		}
	}
	
	public void enableExperimentMode() {
		this.experimentMode = true;
	}
	
	public Set<ProcessModel> evolveProcessModel(ProcessModel model, Goal goal, List<Correction> corrections) {
		Set<ProcessModel> result = new HashSet<ProcessModel>();
		
		if (corrections.isEmpty()) {
			result.add(model);
			return result;
		}  
		if (!validCorrections(model, corrections)) {
			throw new IllegalArgumentException("Invalid corrections");
		}
		
		EngineFactory factory = new EngineFactory();
		CorrectiveEvolutionEngine engine = factory.getEngine(model, goal, corrections);
		engine.setOutputPath(path);
		if (this.experimentMode) {
			engine.enableExperimentMode();
		}
		return engine.solveProblem();
	}
	
	/*
	 * TODO validate all corrections: at each step i, generate process model trivially 
	 * and try to apply correction i+1
	 */
	private boolean validCorrections(ProcessModel model, List<Correction> corrections) {
		if (corrections.isEmpty()) {
			return true;
		}
		
		Correction firstCorr = corrections.get(0);
		if (!firstCorr.isApplicable(model)) {
			logger.error("The first correction is not applicable");
			return false;
		}
		return true;
	}
}
