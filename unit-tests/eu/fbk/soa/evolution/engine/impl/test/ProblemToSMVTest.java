package eu.fbk.soa.evolution.engine.impl.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.log4j.PropertyConfigurator;
import org.junit.Before;
import org.junit.Test;

import eu.fbk.soa.evolution.Correction;
import eu.fbk.soa.evolution.engine.impl.Condition;
import eu.fbk.soa.evolution.engine.impl.ProblemToSMV;
import eu.fbk.soa.evolution.engine.impl.ProblemToSTS;
import eu.fbk.soa.evolution.sts.Action;
import eu.fbk.soa.evolution.sts.STS;
import eu.fbk.soa.evolution.sts.State;
import eu.fbk.soa.evolution.sts.Transition;
import eu.fbk.soa.evolution.sts.nusmv.PathExplorer;
import eu.fbk.soa.process.ProcessModel;
import eu.fbk.soa.util.ConfigUtils;
import eu.fbk.soa.util.IOUtils;

public class ProblemToSMVTest {

	private TestInputProblem problem;
	
	private ProcessModel model;
	
	private List<Correction> corrections;
	
	private ProblemToSMV pb2smv;
	
	@Before
	public void setUpProblem() {
		PropertyConfigurator.configure("log4j.properties");
		problem = new TestInputProblem();
		
		model = problem.getProcessModel();
		
		corrections = new ArrayList<Correction>();
		corrections.add(problem.getCorrection());
	
	}
	
	
	@Test
	public void testModules() {
		pb2smv = new ProblemToSMV(model, corrections);
		String smvStr = pb2smv.translateProblemToSMV();
		String[] tokens = smvStr.split("MODULE");
		int nrOfModules = tokens.length - 1;
				
		assertEquals("There should be 6 separate modules in the smv text", 6, nrOfModules);
	}
	
	@Test
	public void rountripTest() throws IOException {
		ProblemToSTS pb2sts = new ProblemToSTS(model,  new ArrayList<Correction>(), 
				new HashSet<Condition>());
		pb2sts.translateProblem2STS();
		STS modelSTS = pb2sts.getProcessModelSTS(model);
		
		String outDirPath = ConfigUtils.getProperty("outputDir");
		File outDir = new File(outDirPath);
		if (!outDir.exists()) {
			outDir.mkdirs();
		}
		
		pb2smv = new ProblemToSMV(model, new ArrayList<Correction>());
		String smvStr = pb2smv.translateProblemToSMV();
		String smvFileName = outDirPath + File.separator + "simple_pb.smv";
		IOUtils.writeStringToFile(smvStr, smvFileName);

		PathExplorer explorer = new PathExplorer(outDirPath + File.separator);
		STS stsAfterEncoding = explorer.explore(smvFileName);
		
		File intermediaryFile = new File(smvFileName);
		intermediaryFile.delete();
		
		assertTrue("Resulting STSs should only differ in state names and XorSplit actions",
				similarExceptForStateNames(modelSTS, stsAfterEncoding));
	}


	private boolean similarExceptForStateNames(STS sts1, STS sts2) {
		Map<Action, Action> correspAction = new HashMap<Action, Action>();
		Map<Transition, Transition> correspTransition = new HashMap<Transition, Transition>();
		Map<State, State> correspState = new HashMap<State, State>();
		
		boolean inputsMatch = matchInputActions(sts1, sts2, correspAction);
		if (!inputsMatch) {
			return false;
		}
		
		boolean transMatch = matchTransitions(sts1, sts2, correspAction, correspTransition);
		if (!transMatch) {
			return false;
		}
		
		State init1 = sts1.getInitialState();
		correspState.put(init1, sts2.getInitialState());
		
		for (Transition t1 : correspTransition.keySet()) {
			Transition t2 = correspTransition.get(t1);
			
			State corr = correspState.get(t1.getSource());
			if (corr != null && !corr.equals(t2.getSource())) {
				return false;
			}
			correspState.put(t1.getSource(), t2.getSource());
			
			State corrTarget = correspState.get(t1.getTarget());
			if (corrTarget != null && !corrTarget.equals(t2.getTarget())) {
				return false;
			}
			correspState.put(t1.getSource(), t2.getSource());
		}
		
		return true;
	}
	
	private boolean matchTransitions(STS sts1, STS sts2,
			Map<Action, Action> correspAction,
			Map<Transition, Transition> correspTransition) {
		
		for (Transition t1 : sts1.getTransitions()) {
			Action a2 = correspAction.get(t1.getAction());
			if (a2 != null) {
				boolean foundTrans = false;
				for (Transition t2 : sts2.getTransitions()) {
					if (t2.getAction().equals(a2)) {
						correspTransition.put(t1, t2);
						foundTrans = true;
					}
				}
				if (!foundTrans) {
					return false;
				}
			}
		}
		return true;
	}


	private boolean matchInputActions(STS sts1, STS sts2, Map<Action, Action> correspAction) {
		if (sts1.getInputActions().size() != sts2.getInputActions().size()) {
			return false;
		}
		for (Action act1 : sts1.getInputActions()) {
			String actName = act1.getName();
			if (!actName.contains("Xor")) { 
				Action act2 = sts2.getAction(actName);
				if (act2 == null) {
					return false;
				}
				correspAction.put(act1, act2);
			} else {
				boolean foundXor = false;
				for (Action act2 : sts2.getInputActions()) {
					if (act2.getName().contains("Xor")) {
						correspAction.put(act1, act2);
						foundXor = true;
						break;
					}
				}
				if (!foundXor) {
					return false;
				}
			}	
		}
		return true;
	}

}
