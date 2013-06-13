package eu.fbk.soa.evolution.engine.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.PropertyConfigurator;
import org.junit.Before;
import org.junit.Test;

import eu.fbk.soa.evolution.Correction;
import eu.fbk.soa.evolution.Correction.Type;
import eu.fbk.soa.evolution.sts.STS;
import eu.fbk.soa.evolution.sts.State;
import eu.fbk.soa.evolution.sts.Transition;
import eu.fbk.soa.process.Activity;
import eu.fbk.soa.process.Adaptation;
import eu.fbk.soa.process.DefaultProcessModel;
import eu.fbk.soa.process.Effect;
import eu.fbk.soa.process.ProcessModel;
import eu.fbk.soa.process.StateFormula;
import eu.fbk.soa.process.Trace;
import eu.fbk.soa.process.domain.DomainObject;
import eu.fbk.soa.process.domain.ObjectState;
import eu.fbk.soa.process.domain.StateLiteral;
import eu.fbk.soa.process.node.ActivityNode;
import eu.fbk.soa.process.node.ProcessNode;
import eu.fbk.soa.process.node.StartNode;
import eu.fbk.soa.process.node.XorJoin;
import eu.fbk.soa.process.node.XorSplit;

public class ProblemToSTSTest {

	private ProcessModel model;

	private Correction correction;
	
	private Set<Condition> conds;

	private List<Activity> activities;

	private List<ProcessNode> actList;

	private ProblemToSTS pb2sts;
	
	
	@Before
	public void setUpProblem() {
		PropertyConfigurator.configure("log4j.properties");

		model = createProcessModel();

		List<Correction> corrections = new ArrayList<Correction>();
		correction = this.createCorrection();
		corrections.add(correction);

		conds = new HashSet<Condition>();
		conds.add(new Condition(correction.getCondition(), 1));
		pb2sts = new ProblemToSTS(model, corrections, conds);
	}

	/*
	 * Creating the process:
	 * A0 -> XorSplit -> A1 -> A2 -> XorJoin -> A4
	 * 				  -> A3		  ->		
	 */
	private ProcessModel createProcessModel() {
		this.activities = new ArrayList<Activity>();

		Set<ProcessNode> nodes = new HashSet<ProcessNode>();
		actList = new ArrayList<ProcessNode>();
		StartNode start = null;

		for (int i = 0; i <= 4; i++) {
			Activity ai = new Activity("A" + i, new StateFormula(), new Effect());
			activities.add(ai);
			ProcessNode node;
			if (i == 0) {
				start = new StartNode(ai);
				node = start;
			} else {
				node = new ActivityNode(ai);
			}
			actList.add(node);
			nodes.add(node);
		}
		
		XorSplit xorsplit = new XorSplit();
		nodes.add(xorsplit);
		
		XorJoin xorjoin = new XorJoin();
		nodes.add(xorjoin);
		
		ProcessModel model = new DefaultProcessModel("TestProcess", nodes);

		model.addEdge(start, xorsplit);
		model.addEdge(xorsplit, actList.get(1));
		model.addEdge(xorsplit, actList.get(3));
		model.addEdge(actList.get(1), actList.get(2));
		model.addEdge(actList.get(2), xorjoin);
		model.addEdge(actList.get(3), xorjoin);		
		model.addEdge(xorjoin, actList.get(4));
		return model;
	}


	private Correction createCorrection() {
		DomainObject obj = new DomainObject("Obj");
		ObjectState state1 = new ObjectState("State1");
		StateFormula cond = new StateFormula(new StateLiteral(obj, state1));

		int fromActIndex = 1;
		List<Activity> executedActs = new ArrayList<Activity>();
		for (int i = 0; i <= fromActIndex; i++) {
			executedActs.add(activities.get(i));
		}
		Trace trace = new Trace(executedActs);

		ActivityNode fromNode = (ActivityNode) actList.get(fromActIndex);
		ActivityNode toNode = (ActivityNode) actList.get(fromActIndex + 1);

		Set<ProcessNode> adaptNodes = new HashSet<ProcessNode>();
		adaptNodes.add(new ActivityNode("Fix"));
		ProcessModel adaptPM = new DefaultProcessModel("AdaptationModel", adaptNodes);
		Adaptation adaptation = new Adaptation(adaptPM, fromNode, toNode);

		Correction correction = new Correction(Type.STRICT, trace, cond, adaptation);
		return correction;
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
