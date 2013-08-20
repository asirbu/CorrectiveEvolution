package eu.fbk.soa.evolution.sts.minimizer.test;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.PropertyConfigurator;
import org.junit.Before;
import org.junit.Test;

import eu.fbk.soa.evolution.sts.Action;
import eu.fbk.soa.evolution.sts.STS;
import eu.fbk.soa.evolution.sts.State;
import eu.fbk.soa.evolution.sts.Transition;
import eu.fbk.soa.evolution.sts.impl.DefaultAction;
import eu.fbk.soa.evolution.sts.impl.DefaultSTS;
import eu.fbk.soa.evolution.sts.impl.DefaultState;
import eu.fbk.soa.evolution.sts.impl.DefaultTransition;
import eu.fbk.soa.evolution.sts.minimizer.TraceEquivalenceMinimizer;
import eu.fbk.soa.util.ConfigUtils;
import eu.fbk.soa.util.IOUtils;

public class TraceEquivalenceMinimizerTest {

	private STS mySTS;
	
	private String outDir;
	
	@Before 
	public void setUp() throws IOException {
		PropertyConfigurator.configure("log4j.properties");
		
		setUpSimpleSTS();
		
		outDir = ConfigUtils.getProperty("outputDir");
		File outFile = new File(outDir);
		if (!outFile.exists()) {
			outFile.mkdirs();
		}
	}
	
	private void setUpSimpleSTS() {
		Set<State> states = new HashSet<State>();
		State initialState = null;
		Set<Action> inputActions = new HashSet<Action>();
		State previousState = null;
		Set<Transition> transitions = new HashSet<Transition>();
		
		for (int i = 0; i < 10; i++) {
			State state = new DefaultState("s" + i);
			states.add(state);
			if (i == 0) {
				initialState = state;
			
			} else {
				Action action = new DefaultAction("act" + i, true);
				inputActions.add(action);
				
				if (previousState != null) {
					Transition trans = new DefaultTransition(previousState, action, state);
					transitions.add(trans);
				}
			}
			previousState = state;
		}
		
		mySTS = new DefaultSTS(states, initialState, inputActions, new HashSet<Action>(), transitions);
	}
	
	@Test
	public void testMinimizeAlreadyMinimalSTS() {		
		TraceEquivalenceMinimizer minimizer = new TraceEquivalenceMinimizer();
		File traceMin = new File(outDir + File.separator + "min.dot");	
		minimizer.minimizeSTS(mySTS, traceMin);
		
		STS resultSTS = IOUtils.readSTSFromFile(traceMin);
		traceMin.deleteOnExit();
		
		assertEquals("The minimized STS should have the same transitions as the input STS", 
				9, resultSTS.getTransitions().size());
	}
	
	
	@Test
	public void testMinimizeSTS() {
		State initial = mySTS.getInitialState();
		
		Transition trans1 = mySTS.getTransitionsFromState(initial).iterator().next();
		State nextState = trans1.getTarget();
		Transition trans2 = mySTS.getTransitionsFromState(nextState).iterator().next();
		
		State newState = new DefaultState("s10");
		mySTS.addState(newState);
		mySTS.addTransition(new DefaultTransition(initial, trans1.getAction(), newState));
		mySTS.addTransition(new DefaultTransition(newState, trans2.getAction(), trans2.getTarget()));
		
		TraceEquivalenceMinimizer minimizer = new TraceEquivalenceMinimizer();
		File traceMin = new File(outDir + File.separator + "min.dot");	
		minimizer.minimizeSTS(mySTS, traceMin);
		
		STS resultSTS = IOUtils.readSTSFromFile(traceMin);
		traceMin.deleteOnExit();
		
		assertEquals("The transitions added should dissappear in the minimized STS", 
				9, resultSTS.getTransitions().size());
		assertEquals("The state added should dissappear in the minimized STS", 
				10, resultSTS.getStates().size());
	}

}
