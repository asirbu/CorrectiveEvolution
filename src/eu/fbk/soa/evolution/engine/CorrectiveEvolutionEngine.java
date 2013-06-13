package eu.fbk.soa.evolution.engine;

import java.util.Set;

import eu.fbk.soa.process.ProcessModel;


public interface CorrectiveEvolutionEngine {

	public Set<ProcessModel> solveProblem();
	
	public void setOutputPath(String path);
	
	public void enableExperimentMode();
}
