package eu.fbk.soa.evolution.engine.impl.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.log4j.PropertyConfigurator;
import org.junit.Before;
import org.junit.Test;

import eu.fbk.soa.evolution.Correction;
import eu.fbk.soa.evolution.engine.impl.ProblemToSTS;
import eu.fbk.soa.evolution.sts.STS;
import eu.fbk.soa.evolution.sts.State;
import eu.fbk.soa.evolution.sts.Transition;
import eu.fbk.soa.process.Activity;
import eu.fbk.soa.process.ProcessModel;
import eu.fbk.soa.process.Trace;

public class ProblemToSTSTest {

	private ProcessModel model;
	
	private Correction correction;
	
	private ProblemToSTS pb2sts;
	
	private TestInputProblem problem;
	
	@Before
	public void setUpProblem() {
		PropertyConfigurator.configure("log4j.properties");

		problem = new TestInputProblem();
		model = problem.getProcessModel();
		correction = problem.getCorrection();
		
		List<Correction> corrections = new ArrayList<Correction>();
		corrections.add(correction);
		
		pb2sts = new ProblemToSTS(model, corrections, problem.getConditions());
	}


	@Test
	public void testModelToSTS() {
		pb2sts.translateProblem2STS();
		STS modelSTS = pb2sts.getProcessModelSTS(model);
		
		Transition transOnResume = null;
		Set<Transition> transitions = modelSTS.getTransitions();
		for (Transition trans : transitions) {
			if (trans.getActionName().startsWith("Resume")) {
				transOnResume = trans;
			}
		}
		assertNotNull("Correction should realize a jump in the process model STS", transOnResume);
	
		assertEquals("Transition on resume should not move the model STS to a different state", 
				transOnResume.getSource(), transOnResume.getTarget());
	}
	
	
	@Test
	public void testXorToSTS() {
		pb2sts.translateProblem2STS();
		STS modelSTS = pb2sts.getProcessModelSTS(model);
		
		assertEquals("STS should have exactly 2 output actions corresponding to the XorSplit", 
				2, modelSTS.getOutputActions().size());
		
		Set<Transition> transitions = modelSTS.getTransitions();
		Transition t1 = null;
		Transition t2 = null;
		for (Transition trans : transitions) {
			if (trans.getActionName().equals("A2")) {
				t1 = trans;
			}
			if (trans.getActionName().equals("A3")) {
				t2 = trans;
			}
		}
		assertEquals("The transitions on A2 and A3 lead to the same state", 
				t1.getTarget(), t2.getTarget());
	}
	
	@Test
	public void testTraceToSTS() {
		Trace trace = correction.getTrace();
		Activity firstActivity = trace.getActivity(0);
		
		pb2sts.translateProblem2STS();
		STS traceSTS = pb2sts.getTraceSTS(trace);
		State initial = traceSTS.getInitialState();
		
		Set<Transition> transFromInitial = traceSTS.getTransitionsFromState(initial);
		Transition transTrace = null;
		Transition transOut = null;
		for (Transition trans : transFromInitial) {
			if (trans.getActionName().equals(firstActivity.getName())) {
				transTrace = trans;
			}
			if (trans.getTarget().getName().equals("tout")) {
				transOut = trans;
			}
		}
		assertEquals("There should be exactly 2 transitions from the initial state", 2, transFromInitial.size());
		
		assertNotNull("A transition corresponds to the first activity in the trace", transTrace);
		
		assertNotNull("A transition leads to the out state", transOut);
	}
}
