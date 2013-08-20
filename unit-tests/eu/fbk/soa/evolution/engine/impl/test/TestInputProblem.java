package eu.fbk.soa.evolution.engine.impl.test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import eu.fbk.soa.evolution.Correction;
import eu.fbk.soa.evolution.Correction.Type;
import eu.fbk.soa.evolution.engine.impl.Condition;
import eu.fbk.soa.evolution.engine.impl.ProblemToSTS;
import eu.fbk.soa.process.Activity;
import eu.fbk.soa.process.Adaptation;
import eu.fbk.soa.process.DefaultProcessModel;
import eu.fbk.soa.process.Effect;
import eu.fbk.soa.process.ProcessModel;
import eu.fbk.soa.process.StateFormula;
import eu.fbk.soa.process.Trace;
import eu.fbk.soa.process.domain.DomainObject;
import eu.fbk.soa.process.domain.ObjectEvent;
import eu.fbk.soa.process.domain.ObjectState;
import eu.fbk.soa.process.domain.ObjectTransition;
import eu.fbk.soa.process.domain.StateLiteral;
import eu.fbk.soa.process.node.ActivityNode;
import eu.fbk.soa.process.node.ProcessNode;
import eu.fbk.soa.process.node.StartNode;
import eu.fbk.soa.process.node.XorJoin;
import eu.fbk.soa.process.node.XorSplit;

public class TestInputProblem {

	private ProcessModel model;

	private Correction correction;
	
	private Set<Condition> conds;

	private List<Activity> activities;

	public Correction getCorrection() {
		return correction;
	}

	public Set<Condition> getConditions() {
		return conds;
	}

	private List<ProcessNode> actList;

	private ProblemToSTS pb2sts;
	
	
	public TestInputProblem() {
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
		
		ObjectState state1 = new ObjectState("State1");
		Set<ObjectState> states = new HashSet<ObjectState>();
		states.add(state1);
		DomainObject obj = new DomainObject("Obj", states,
				new HashSet<ObjectState>(), new HashSet<ObjectEvent>(),
				new HashSet<ObjectTransition>());
		
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
	
	public ProcessModel getProcessModel() {
		return this.model;
	}
	
}
