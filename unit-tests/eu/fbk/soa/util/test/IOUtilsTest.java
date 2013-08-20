package eu.fbk.soa.util.test;

import static org.junit.Assert.assertTrue;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

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
import eu.fbk.soa.util.IOUtils;

public class IOUtilsTest {

	private STS mySTS;
	
	@Before
	public void setUpSimpleSTS() {
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
	public void testCreateImage() {

		try {
			File dotFile = File.createTempFile("mySTS", "dot");
			PrintWriter out = new PrintWriter(new FileWriter(dotFile));
			out.println(mySTS.toDot());
			out.flush();
			out.close();
			
			File outFile = File.createTempFile("mySTS", "png");
			IOUtils.createImage(dotFile.getPath(), outFile.getPath());
			
			BufferedInputStream stream = new BufferedInputStream(new FileInputStream(outFile));
			assertTrue("Image file should not be empty", stream.available() > 0);
		
			stream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	
	}
}
