package eu.fbk.soa.eventlog.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.PropertyConfigurator;
import org.junit.Before;
import org.junit.Test;

import eu.fbk.soa.eventlog.TraceDifference;
import eu.fbk.soa.eventlog.TraceReplay;
import eu.fbk.soa.process.Activity;
import eu.fbk.soa.process.DefaultProcessModel;
import eu.fbk.soa.process.Effect;
import eu.fbk.soa.process.ProcessModel;
import eu.fbk.soa.process.StateFormula;
import eu.fbk.soa.process.node.ActivityNode;
import eu.fbk.soa.process.node.AndJoin;
import eu.fbk.soa.process.node.AndSplit;
import eu.fbk.soa.process.node.ControlConnector;
import eu.fbk.soa.process.node.ProcessNode;
import eu.fbk.soa.process.node.StartNode;
import eu.fbk.soa.process.node.XorJoin;
import eu.fbk.soa.process.node.XorSplit;


public class TraceReplayTest {

	private ProcessModel model;
	
	private Set<Activity> activities;
	
	@Before
	public void setUp() {
		PropertyConfigurator.configure("log4j.properties");
		createProcessModel();
	}
	
	
	/*
	 * Creating the process:
	 * A0 -> A1 -> XorSplit -> A2 -> A3 -> XorJoin -> AndSplit -> A5 -> A6 -> AndJoin -> A8
	 * 						-> A4		->						-> A7 		->
	 */
	private void createProcessModel() {
		this.activities = new HashSet<Activity>();
		
		Set<ProcessNode> nodes = new HashSet<ProcessNode>();
		List<ProcessNode> actList = new ArrayList<ProcessNode>();
		StartNode start = null;
		
		for (int i = 0; i <= 8; i++) {
			Activity ai = new Activity("A" + i, new StateFormula(), new Effect());
			activities.add(ai);
			ProcessNode node;
			if (i == 0) {
				start = new StartNode(ai);
				node = start;
			} else {
				node = new ActivityNode(ai);
				actList.add(node);
			}
			nodes.add(node);
		}
		
		XorSplit xorsplit = new XorSplit();
		nodes.add(xorsplit);
		
		XorJoin xorjoin = new XorJoin();
		nodes.add(xorjoin);
		
		AndSplit andsplit = new AndSplit();
		nodes.add(andsplit);
		
		AndJoin andjoin = new AndJoin();
		nodes.add(andjoin);
		
		model = new DefaultProcessModel("TestProcess", nodes);
		
		model.addEdge(start, actList.get(0));
		model.addEdge(actList.get(0), xorsplit);
		
		connectSplitToNodes(xorsplit, actList.get(1), actList.get(3));
		model.addEdge(actList.get(1), actList.get(2));
		
		connectNodesToJoin(actList.get(2), actList.get(3), xorjoin);
		
		model.addEdge(xorjoin, andsplit); 
		
		connectSplitToNodes(andsplit, actList.get(4), actList.get(6));
		
		model.addEdge(actList.get(4), actList.get(5));
				
		connectNodesToJoin(actList.get(5), actList.get(6), andjoin);
		
		model.addEdge(andjoin, actList.get(7)); 
	}
	
	private void connectSplitToNodes(
			ControlConnector split, ProcessNode node1, ProcessNode node2) {
		model.addEdge(split, node1);
		model.addEdge(split, node2);
	}
	
	private void connectNodesToJoin(
			ProcessNode node1, ProcessNode node2, ControlConnector join) {
		model.addEdge(node1, join);
		model.addEdge(node2, join);
	}
	
	
	@Test
	public void testMatchingTrace() {
		TraceReplay replay = new TraceReplay(model, activities);
		
		String[] traceActs = {"A0", "A1", "A2", "A3", "A5", "A7", "A6", "A8"};
		List<String> trace = new ArrayList<String>();
		for (String str : traceActs) {
			trace.add(str);
		}
		
		List<TraceDifference> diffs = replay.computeDifferences(trace);
		assertTrue(diffs.isEmpty());
		
		String[] traceActs2 = {"A0", "A1", "A4", "A7", "A5", "A6", "A8"};
		trace = new ArrayList<String>();
		for (String str : traceActs2) {
			trace.add(str);
		}
		
		diffs = replay.computeDifferences(trace);
		assertTrue(diffs.isEmpty());
		
	}

	@Test
	public void testDifferentTrace() {
		activities.add(new Activity("A9", new StateFormula(), new Effect()));
		TraceReplay replay = new TraceReplay(model, activities);
		
		String[] traceActs = {"A0",  "A1", "A2",  "A3", "A5", "A9", "A7", "A6", "A8"};
		List<String> trace = new ArrayList<String>();
		for (String str : traceActs) {
			trace.add(str);
		}
		
		List<TraceDifference> diffs = replay.computeDifferences(trace);
		
		System.out.println("\ntestDifferentTrace:");
		for (TraceDifference diff : diffs) {
			System.out.println(diff);
		}
		
		assertEquals(1, diffs.size());
	}

	@Test
	public void testDifferenceAtEnd() {
		activities.add(new Activity("A9", new StateFormula(), new Effect()));
		
		TraceReplay replay = new TraceReplay(model, activities);
		
		String[] traceActs = {"A0",  "A1", "A2",  "A3", "A5", "A7", "A6", "A8", "A9"};
		List<String> trace = new ArrayList<String>();
		for (String str : traceActs) {
			trace.add(str);
		}
		
		List<TraceDifference> diffs = replay.computeDifferences(trace);
		
		System.out.println("\ntestDifferenceAtEnd:");
		for (TraceDifference diff : diffs) {
			System.out.println(diff);
		}
		
		assertEquals(1, diffs.size());
	}
	
	
}
