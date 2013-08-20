package eu.fbk.soa.evolution.test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.fbk.soa.evolution.Correction;
import eu.fbk.soa.evolution.Correction.Type;
import eu.fbk.soa.process.Activity;
import eu.fbk.soa.process.Adaptation;
import eu.fbk.soa.process.DefaultProcessModel;
import eu.fbk.soa.process.EaGLeGoal;
import eu.fbk.soa.process.EaGLeGoal.Operator;
import eu.fbk.soa.process.Effect;
import eu.fbk.soa.process.ProcessEdge;
import eu.fbk.soa.process.ProcessModel;
import eu.fbk.soa.process.StateFormula;
import eu.fbk.soa.process.StateLiteralClause;
import eu.fbk.soa.process.Trace;
import eu.fbk.soa.process.domain.DomainObject;
import eu.fbk.soa.process.domain.EventLiteral;
import eu.fbk.soa.process.domain.ObjectEvent;
import eu.fbk.soa.process.domain.ObjectState;
import eu.fbk.soa.process.domain.ObjectTransition;
import eu.fbk.soa.process.domain.StateLiteral;
import eu.fbk.soa.process.node.ActivityNode;
import eu.fbk.soa.process.node.EndNode;
import eu.fbk.soa.process.node.ProcessNode;
import eu.fbk.soa.process.node.StartNode;
import eu.fbk.soa.process.node.XorJoin;
import eu.fbk.soa.process.node.XorSplit;

public class InputConstructor {

	public static DomainObject constructSimpleDomainObject(String name, 
			ObjectState initState, ObjectState nextState, 
			ObjectEvent doEvent, ObjectEvent undoEvent) {
		
		Set<ObjectState> totalSt = new HashSet<ObjectState>();
		totalSt.add(initState);
		totalSt.add(nextState);
		Set<ObjectState> initialSt = new HashSet<ObjectState>();
		initialSt.add(initState);
		
		Set<ObjectEvent> events = new HashSet<ObjectEvent>();
		events.add(doEvent);
		events.add(undoEvent);
		
		ObjectTransition t1 = new ObjectTransition(initState, doEvent, nextState);
		ObjectTransition t2 = new ObjectTransition(nextState, undoEvent, initState);
		Set<ObjectTransition> transitions = new HashSet<ObjectTransition>();
		transitions.add(t1);
		transitions.add(t2);
		
		return new DomainObject(name, totalSt, initialSt, events, transitions);
	}
	
	public static DomainObject constructSimpleDomainObject(String name, 
			ObjectState initState, ObjectState nextState) {
		
		ObjectEvent doEvent = new ObjectEvent("do");
		ObjectEvent undoEvent = new ObjectEvent("undo");
		
		return InputConstructor.constructSimpleDomainObject(
				name, initState, nextState, doEvent, undoEvent);
	}
	

	public static ProcessModel construct2XorProcessModel() {
		ObjectState ok = new ObjectState("OK");
		ObjectState dmg = new ObjectState("Damaged");
		DomainObject health = constructSimpleDomainObject("CarHealth", ok, dmg);
		
		ObjectState full = new ObjectState("Full");
		ObjectState empty = new ObjectState("Empty");
		DomainObject queue = constructSimpleDomainObject("Queue", empty, full);
		
		ProcessNode start = new StartNode();
		ProcessNode show = new ActivityNode("Show route to storage");
		ProcessNode xorsplit1 = new XorSplit();
		ProcessNode assess = new ActivityNode("Assess damage");
		ProcessNode xorsplit2 = new XorSplit();
		ProcessNode xorjoin2 = new XorJoin();
		ProcessNode repair = new ActivityNode("Repair");
		ProcessNode postpone = new ActivityNode("Postpone");
		ProcessNode xorjoin1 = new XorJoin();
		ProcessNode atstg = new ActivityNode("At storage");
		ProcessNode end = new EndNode();
		
		Set<ProcessNode> nodes = new HashSet<ProcessNode>();
		nodes.add(start);
		nodes.add(show);
		nodes.add(xorsplit1);
		nodes.add(assess);
		nodes.add(xorsplit2);
		nodes.add(xorjoin2);
		nodes.add(repair);
		nodes.add(postpone);
		nodes.add(xorjoin1);
		nodes.add(atstg);
		nodes.add(end);
		
		ProcessModel model = new DefaultProcessModel("TestProcess", nodes);
		model.addEdge(start, show);
		model.addEdge(show, xorsplit1);
		model.addEdge(xorsplit1, assess, 
				new StateFormula(new StateLiteralClause(new StateLiteral(health, dmg))));
		model.addEdge(assess, xorsplit2);
		model.addEdge(xorsplit2, repair, 
				new StateFormula(new StateLiteralClause(new StateLiteral(queue, empty))));
		model.addEdge(xorsplit2, postpone, 
				new StateFormula(new StateLiteralClause(new StateLiteral(queue, full))));
		model.addEdge(repair, xorjoin2);
		model.addEdge(postpone, xorjoin2);
		
		model.addEdge(xorjoin2, xorjoin1);
		model.addEdge(xorsplit1, xorjoin1, 
				new StateFormula(new StateLiteralClause(new StateLiteral(health, ok))));
		model.addEdge(xorjoin1, atstg);
		model.addEdge(atstg, end);
		
		return model;
	}
	
	public static ProcessModel constructSimpleLogisticsProcessModel() {
		ObjectState ok = new ObjectState("OK");
		ObjectState dmg = new ObjectState("Damaged");
		DomainObject health = constructSimpleDomainObject("CarHealth", ok, dmg);
		StateLiteral okLit = new StateLiteral(health, ok);
		
		ObjectState noRoute = new ObjectState("NoRoute");
		ObjectState route = new ObjectState("Route");
		ObjectEvent show = new ObjectEvent("show");
		ObjectEvent arrive = new ObjectEvent("arrive");
		DomainObject navi = constructSimpleDomainObject("Navigation", noRoute, 
				route, show, arrive);
		
		List<ProcessNode> nodeList = new ArrayList<ProcessNode>();
		nodeList.add(new StartNode());
		
		Activity showAct = new Activity("Show route to storage", 
				new StateFormula(okLit, new StateLiteral(navi, noRoute)), 
				new Effect(new EventLiteral(navi, show)));
		nodeList.add(new ActivityNode(showAct));
		
		Activity atStg = new Activity("At storage", 
				new StateFormula(okLit, new StateLiteral(navi, route)), 
				new Effect(new EventLiteral(navi, arrive)));
		nodeList.add(new ActivityNode(atStg));
		
		Activity rec = new Activity("Receive delivery order",
				new StateFormula(okLit), new Effect());
		nodeList.add(new ActivityNode(rec));
		
		nodeList.add(new ActivityNode("Fix pending treatments"));
		nodeList.add(new ActivityNode("Deliver to retailer"));
		nodeList.add(new EndNode());
		
		ProcessModel model = new DefaultProcessModel("CarProcess", nodeList);
		for (int i = 0; i < nodeList.size() - 1; i++) {
			model.addEdge(nodeList.get(i), nodeList.get(i + 1));
		}
		return model;
	}

	
	public static Map<String, ProcessModel> constructLogisticsProcessModels() {
		Map<String, ProcessModel> processes = new HashMap<String, ProcessModel>();
		
		// Construct the CarHealth domain object
		ObjectState ok = new ObjectState("OK");
		ObjectState dmg = new ObjectState("Damaged");
		ObjectState diag = new ObjectState("Diagnosed");

		Set<ObjectState> totalSt = new HashSet<ObjectState>();
		totalSt.add(ok); totalSt.add(dmg); totalSt.add(diag);
		Set<ObjectState> initialSt = new HashSet<ObjectState>();
		initialSt.add(ok);

		ObjectEvent damage = new ObjectEvent("damage", false);
		ObjectEvent repair = new ObjectEvent("repair");
		ObjectEvent diagnose = new ObjectEvent("diagnose");
		Set<ObjectEvent> events = new HashSet<ObjectEvent>();
		events.add(damage); events.add(repair); events.add(diagnose);
		
		Set<ObjectTransition> transitions = new HashSet<ObjectTransition>();
		transitions.add(new ObjectTransition(ok, damage, dmg));
		transitions.add(new ObjectTransition(dmg, diagnose, diag));
		transitions.add(new ObjectTransition(diag, repair, ok));
		transitions.add(new ObjectTransition(ok, repair, ok));
		transitions.add(new ObjectTransition(diag, damage, dmg));
		
		DomainObject health = new DomainObject("CarHealth", totalSt, initialSt, events, transitions);
		StateLiteral okLit = new StateLiteral(health, ok);
		StateLiteral dmgLit = new StateLiteral(health, dmg);
		StateLiteral diagnosedLit = new StateLiteral(health, diag);
		StateLiteral notDmgLit = dmgLit.getNegation();
		EventLiteral diagnoseLit = new EventLiteral(health, diagnose);
		EventLiteral repairLit = new EventLiteral(health, repair);
		
		ObjectState storage = new ObjectState("Storage");
		ObjectState retailer = new ObjectState("Retailer");
		ObjectEvent move2Ret = new ObjectEvent("move2Ret");
		ObjectEvent move2Stg = new ObjectEvent("move2Stg");
		DomainObject location = constructSimpleDomainObject(
				"CarLocation", storage, retailer, move2Ret, move2Stg);
		
		ObjectState noRoute = new ObjectState("NoRoute");
		ObjectState route = new ObjectState("Route");
		ObjectEvent show = new ObjectEvent("show");
		ObjectEvent arrive = new ObjectEvent("arrive");
		DomainObject navi = constructSimpleDomainObject("Navigation", noRoute, 
				route, show, arrive);
				
		List<Activity> actList = new ArrayList<Activity>();
		actList.add(new Activity("Show route to storage", 
				new StateFormula(notDmgLit, new StateLiteral(navi, noRoute)), 
				new Effect(new EventLiteral(navi, show))));
		
		actList.add(new Activity("At storage", 
				new StateFormula(notDmgLit, new StateLiteral(navi, route)), 
				new Effect(new EventLiteral(navi, arrive))));
		
		actList.add(new Activity("Receive delivery order",
				new StateFormula(notDmgLit), new Effect()));
		
		actList.add(new Activity("Fix pending treatments",
				new StateFormula(notDmgLit), new Effect(new EventLiteral(health, repair))));
		
		actList.add(new Activity("Deliver to retailer",
				new StateFormula(okLit), new Effect(new EventLiteral(location, move2Ret))));
		
		ProcessModel model = constructSequenceProcessModel("CarProcess", actList);
		processes.put("CarProcess", model);
		
		Set<ProcessNode> adaptNodes = new HashSet<ProcessNode>();
		Activity assess = new Activity("Assess damage", 
				new StateFormula(dmgLit), new Effect(diagnoseLit));
		adaptNodes.add(new ActivityNode(assess));
		ProcessModel adaptPM = new DefaultProcessModel("ScheduleRepair", adaptNodes);
		processes.put("ScheduleRepair", adaptPM);
		
		List<Activity> actList2 = new ArrayList<Activity>();		
		actList2.add(new Activity("Temp assess damage", 
				new StateFormula(dmgLit), new Effect(diagnoseLit)));
		
		actList2.add(new Activity("Temp fix damages", 
				new StateFormula(diagnosedLit), new Effect(repairLit)));
		
		ProcessModel adaptPM2 = constructSequenceProcessModel("TempRepair", actList2);
		processes.put("TempRepair", adaptPM2);
		
		return processes;
	}

	private static ProcessModel constructSequenceProcessModel(String name, List<Activity> actList) {
		List<ProcessNode> nodeList = new ArrayList<ProcessNode>();
		nodeList.add(new StartNode());
		
		for (Activity act : actList) {
			nodeList.add(new ActivityNode(act));
		}
		nodeList.add(new EndNode());
		
		ProcessModel model = new DefaultProcessModel(name, nodeList);
		for (int i = 0; i < nodeList.size() - 1; i++) {
			model.addEdge(nodeList.get(i), nodeList.get(i + 1));
		}
		return model;
	}
	
	public static EaGLeGoal constructLogisticsGoal(Map<String, ProcessModel> models) {
		ProcessModel model = models.get("CarProcess");
		Set<DomainObject> domainObjects = model.getRelatedDomainObjects();
		
		DomainObject health = getDomainObjectByName(domainObjects, "CarHealth");
		ObjectState ok = null;
		if (health != null) {
			ok = health.getStateByName("OK");
		}
		
		DomainObject location = getDomainObjectByName(domainObjects, "CarLocation");
		ObjectState retailer = null;
		if (location != null) {
			retailer = location.getStateByName("Retailer");
		}
		return new EaGLeGoal(Operator.DoReach, new StateFormula(
				new StateLiteral(health, ok), new StateLiteral(location, retailer)));
	}
	
	
	private static DomainObject getDomainObjectByName(
			Set<DomainObject> domainObjects, String name) {
		
		for (DomainObject obj : domainObjects) {
			if (obj.getName().equals(name)) {
				return obj;
			}
		}
		return null;
	}
	
	public static List<Correction> constructLogisticsCorrections(
			Map<String, ProcessModel> models, Type corrType1, Type corrType2) {
		
		List<Correction> corrections = new ArrayList<Correction>();
		ProcessModel model = models.get("CarProcess");
		
		Set<DomainObject> domainObjects = model.getRelatedDomainObjects();
		DomainObject health = getDomainObjectByName(domainObjects, "CarHealth");
		ObjectState dmg = null;
		if (health != null) {
			dmg = health.getStateByName("Damaged");
		}
		StateFormula cond = new StateFormula(new StateLiteral(health, dmg));
		

		ProcessNode startNode = model.getStartNode();		
		ActivityNode fromNode = getNextActivityNode(startNode, model);
		ActivityNode toNode = getNextActivityNode(fromNode, model);
		Trace trace = getTrace(corrType1, startNode, fromNode);
		
		ProcessModel adaptPM1 = models.get("ScheduleRepair");
		Adaptation adaptation1 = new Adaptation(adaptPM1, fromNode, toNode);
		Correction correction1 = new Correction(corrType1, trace, cond, adaptation1);
		corrections.add(correction1);
		
		
		ActivityNode adNode = (ActivityNode) adaptPM1.getProcessNodes().iterator().next();
		Trace trace2 = getTrace(corrType2, startNode, fromNode, adNode, toNode);
		
		ProcessModel adaptPM2 = models.get("TempRepair");
		Adaptation adaptation2 = new Adaptation(adaptPM2, toNode, fromNode);
		Correction correction2 = new Correction(corrType2, trace2, cond, adaptation2);
		corrections.add(correction2);
		
		return corrections;
	}
	
	private static Trace getTrace(Type corrType, ProcessNode...nodes) {
		Trace trace = new Trace();
		
		if (corrType == Type.STRICT) {
			for (ProcessNode node : nodes) {
				if (node instanceof ActivityNode) {
					trace.addActivity(((ActivityNode) node).getActivity());
				}
			}
		}
		return trace;
	}
	
	private static ActivityNode getNextActivityNode(ProcessNode node, ProcessModel model) {
		ProcessEdge edge = model.outgoingEdgesOf(node).iterator().next();
		return (ActivityNode) model.getEdgeTarget(edge);
	}
	
	public static List<Correction> constructLogisticsCorrectionsWithQueue(
			Set<DomainObject> domainObjects, 
			Map<String, ProcessModel> models, Type corrType1, Type corrType2) {
		
		ProcessModel model = models.get("CarProcess");
		
		DomainObject health = getDomainObjectByName(domainObjects, "CarHealth");
		ObjectState dmg = null;
		if (health != null) {
			dmg = health.getStateByName("Damaged");
		}
		
		DomainObject queue = getDomainObjectByName(domainObjects, "TreatmentQueue");
		ObjectState full = null;
		if (queue != null) {
			full = queue.getStateByName("Full");
		}
		
		List<Activity> actList = new ArrayList<Activity>();
		Activity start = model.getStartNode().getActivity();
		actList.add(start);
		
		ProcessEdge edge = model.outgoingEdgesOf(model.getStartNode()).iterator().next();
		ActivityNode fromNode = (ActivityNode) model.getEdgeTarget(edge);
		actList.add(fromNode.getActivity());
//		System.out.println("Precondition: " + fromNode.getActivity().getPrecondition());
		
		ProcessEdge edge2 = model.outgoingEdgesOf(fromNode).iterator().next();
		ActivityNode toNode = (ActivityNode) model.getEdgeTarget(edge2);
		Trace trace = new Trace();
		if (corrType1 == Type.STRICT) {
			trace = new Trace(actList);
		}
		
		StateFormula cond1 = new StateFormula(new StateLiteral(health, dmg),
				new StateLiteral(queue, full));
		StateFormula cond2 = new StateFormula(new StateLiteral(health, dmg));
		
		
		ProcessModel adaptPM = models.get("ScheduleRepair");
		Adaptation adaptation = new Adaptation(adaptPM, fromNode, toNode);
		Correction correction = new Correction(corrType1, trace, cond1, adaptation);
		
		List<Activity> actList2 = new ArrayList<Activity>(actList);
		ActivityNode adNode = (ActivityNode) adaptPM.getProcessNodes().iterator().next();
		actList2.add(adNode.getActivity());
		actList2.add(toNode.getActivity());
		Trace trace2 = new Trace();
		if (corrType2 == Type.STRICT) {
			trace2 = new Trace(actList2);
		}
		
		ProcessModel adaptPM2 = models.get("TempRepair");
		Adaptation adaptation2 = new Adaptation(adaptPM2, toNode, fromNode);
		Correction correction2 = new Correction(corrType2, trace2, cond2, adaptation2);
		
		List<Correction> corrections = new ArrayList<Correction>();
		corrections.add(correction);
		corrections.add(correction2);
		return corrections;
	}
	
	
	public static Correction constructSimpleLogisticsCorrection(ProcessModel model) {
		
		Set<DomainObject> domainObjects = model.getRelatedDomainObjects();
		DomainObject health = null;
		ObjectState dmg = null;
		for (DomainObject obj : domainObjects) {
			if (obj.getName().equals("CarHealth")) {
				health = obj;
				Set<ObjectState> states = obj.getStates();
				for (ObjectState state : states) {
					if (state.getName().equals("Damaged")) {
						dmg = state;
					}
				}
			}
		}
		
		List<Activity> actList = new ArrayList<Activity>();
		Activity start = model.getStartNode().getActivity();
		actList.add(start);
		
		ProcessEdge edge = model.outgoingEdgesOf(model.getStartNode()).iterator().next();
		ActivityNode fromNode = (ActivityNode) model.getEdgeTarget(edge);
		actList.add(fromNode.getActivity());
//		System.out.println("Precondition: " + fromNode.getActivity().getPrecondition());
		
		ProcessEdge edge2 = model.outgoingEdgesOf(fromNode).iterator().next();
		ActivityNode toNode = (ActivityNode) model.getEdgeTarget(edge2);
		
		Trace trace = new Trace(actList);
		
		StateFormula cond = new StateFormula(new StateLiteral(health, dmg));
		
		Set<ProcessNode> adaptNodes = new HashSet<ProcessNode>();
		adaptNodes.add(new ActivityNode("Assess damage"));
		ProcessModel adaptPM = new DefaultProcessModel("ScheduleRepair", adaptNodes);
		Adaptation adaptation = new Adaptation(adaptPM, fromNode, toNode);
		
		Correction correction = new Correction(Type.STRICT, trace, cond, adaptation);
		return correction;
	}
	
	
}
