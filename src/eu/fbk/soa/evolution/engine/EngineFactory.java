package eu.fbk.soa.evolution.engine;

import java.util.Collection;
import java.util.List;

import eu.fbk.soa.evolution.Correction;
import eu.fbk.soa.process.Goal;
import eu.fbk.soa.process.ProcessModel;

public class EngineFactory {
	
	public CorrectiveEvolutionEngine getEngine(ProcessModel model, Goal goal,
			List<Correction> corrections) {
		
		CorrectiveEvolutionEngine engine = null;
		
		if (evolutionProblemIsStrict(corrections)) {
			engine = new StrictEngine(model, corrections);
		} else {
			if (evolutionProblemIsRelaxed(corrections)) {
				engine = new RelaxedEngine(model, goal, corrections);
			} else {
				throw new UnsupportedOperationException("Solving " +
				"a general corrective evolution problem is not implemented.");
			}
		}
		return engine;
	}

	private boolean evolutionProblemIsStrict(Collection<Correction> corrections) {
		for (Correction corr : corrections) {
			if (!corr.isStrict()) {
				return false;
			}
		}
		return true;
	}
	
	private boolean evolutionProblemIsRelaxed(Collection<Correction> corrections) {
		for (Correction corr : corrections) {
			if (corr.isRelaxedWithConditions()) {
				return false;
			}
		}
		return true;
	}
}
