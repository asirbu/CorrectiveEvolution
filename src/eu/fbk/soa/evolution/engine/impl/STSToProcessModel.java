package eu.fbk.soa.evolution.engine.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import eu.fbk.soa.evolution.sts.Action;
import eu.fbk.soa.evolution.sts.STS;
import eu.fbk.soa.evolution.sts.State;
import eu.fbk.soa.evolution.sts.Transition;
import eu.fbk.soa.evolution.sts.impl.DefaultAction;
import eu.fbk.soa.evolution.sts.impl.DefaultTransition;
import eu.fbk.soa.process.Activity;
import eu.fbk.soa.process.DefaultProcessModel;
import eu.fbk.soa.process.ProcessEdge;
import eu.fbk.soa.process.ProcessModel;
import eu.fbk.soa.process.StateFormula;
import eu.fbk.soa.process.node.ActivityNode;
import eu.fbk.soa.process.node.ProcessNode;
import eu.fbk.soa.process.node.XorJoin;
import eu.fbk.soa.process.node.XorSplit;

public class STSToProcessModel {
	
	static Logger logger = Logger.getLogger(STSToProcessModel.class);
	
	static int index = 0;
	
	private DefaultProcessModel newPM;
	
	private Map<Action, Activity> action2activity = new HashMap<Action, Activity>();
	
	private Map<Action, Condition> action2condition = new HashMap<Action, Condition>();
	
	private Map<Transition, ProcessNode> trans2node;
	
	private STS simplifiedSTS;

	private Map<Transition, StateFormula> trans2cond;
	
	private static String defaultName = "Action";
	
	public ProcessModel sts2ProcessModel(STS sts, 
			Map<Action, Activity> actionMap, Map<Action, Condition> action2condition) {
		this.newPM = new DefaultProcessModel("CorrectedProcess");
		this.action2activity = actionMap;
		this.action2condition = action2condition;
		trans2node = new HashMap<Transition, ProcessNode>();
		trans2cond = new HashMap<Transition, StateFormula>();
		simplifiedSTS = sts.getCopy();
		
//		this.mergeTransitons(simplifiedSTS);
		
		this.addNodes();
		
		this.addEdges();
		
		this.addXorJoinNodes();
		
		this.addXorSplitNodes();
		
		int activityNodes = 0;
		for (ProcessNode node : newPM.vertexSet()) {
			if (node instanceof ActivityNode) {
				activityNodes++;
			}
		}
		logger.trace("Process model has " + activityNodes + " activity nodes");
	
		for (ProcessNode node : newPM.vertexSet()) {
			if (newPM.inDegreeOf(node) == 0 && node instanceof ActivityNode) {
				newPM.setStartNode((ActivityNode) node);
				break;
			}
		}
		
		simplifiedSTS.removeUnusedActions();
		simplifiedSTS.removeUnusedStates();
		return newPM;
	}
	
	
	
//	private void mergeTransitions(STS sts) {
//		Map<Transition, Transition> toMerge = new HashMap<Transition, Transition>(); 
//		
//		for (Transition t1 : sts.getTransitions()) {
//			if (!t1.getAction().getName().contains("Trigger")) {
//				continue;
//			}
//			Set<Transition> sameSource = sts.getTransitionsFromState(t1.getSource());
//			for (Transition t2 : sameSource) {
//				if (!t2.getAction().getName().contains("Trigger")) {
//					continue;
//				}
//				if (!t1.equals(t2) && t1.getTarget().equals(t2.getTarget())) {
//					if (!toMerge.containsKey(t2)) {
//						toMerge.put(t1, t2);
//					}
//				}
//			}
//		}
//		
//		for (Transition t1 : toMerge.keySet()) {
//			simplifiedSTS.removeTransition(toMerge.get(t1));
//			simplifiedSTS.removeTransition(t1);
//			StateFormula cond = StateFormula.createDisjunction(t1.getCondition(), toMerge.get(t1).getCondition());
//			t1.setCondition(cond);
//			simplifiedSTS.addTransition(t1);
//		}
//	}



	// TODO transitions that do not translate to activities: when should they be
	// removed, and when should they be left in? 
	private void addNodes() {
		List<Transition> transitionsToVisit = new ArrayList<Transition>(
				simplifiedSTS.getTransitions());

		while (!transitionsToVisit.isEmpty()) {
			Transition trans = transitionsToVisit.remove(0);
			logger.trace("Visiting " + trans);
			
			boolean added = addAsActivityNode(trans) || addAsXorSplit(trans);
			
			if (!added) {
				
				State source = trans.getSource();
				State target = trans.getTarget();
				
				Set<Transition> outTrans = simplifiedSTS.getTransitionsFromState(source);
				Set<Transition> inTrans = simplifiedSTS.getTransitionsToState(target);
				simplifiedSTS.removeTransition(trans);
				
				if (outTrans.size() > 1 || inTrans.size() > 1) {
					Transition newTrans = new DefaultTransition(source, trans.getCondition(), 
							new DefaultAction(defaultName, true), target);
					simplifiedSTS.addTransition(newTrans);
					logger.trace("Substituting " + trans + " with " + newTrans);
				} else {
					
					replaceState(target, source);
					Set<Transition> copy = new HashSet<Transition>(transitionsToVisit);
					for (Transition t : copy) {
						Transition newT = t.replaceState(target, source);
						if (!newT.equals(t)) {
							transitionsToVisit.remove(t);
							transitionsToVisit.add(newT);
						}
					}
				}
			}	
		}				
	}

	private void replaceState(State state, State replacement) {
		logger.trace("Replacing state " + state.getName() + 
				" with " + replacement.getName());
		simplifiedSTS.replaceState(state, replacement);
		simplifiedSTS.removeState(state);
			
		Set<Transition> mappedTransitions = new HashSet<Transition>(
				trans2node.keySet());
		for (Transition trans : mappedTransitions) {
			Transition newTrans = trans.replaceState(state, replacement);
			if (!newTrans.equals(trans)) {
				trans2node.put(newTrans, trans2node.get(trans));
				trans2node.remove(trans);
			}
		}

		Set<Transition> condTransitions = new HashSet<Transition>(
				this.trans2cond.keySet());
		for (Transition trans : condTransitions) {
			Transition newTrans = trans.replaceState(state, replacement);
			if (!newTrans.equals(trans)) {
				trans2cond.put(newTrans, trans2cond.get(trans));
				trans2cond.remove(trans);
			}
		}	
	}
	
	
	private boolean addAsActivityNode(Transition trans) {
		Activity act = this.getCorrespondingActivity(trans);
		if (act != null) {
			ActivityNode actNode = new ActivityNode(act);
			newPM.addVertex(actNode);
			trans2node.put(trans, actNode);
			return true;
		}
		return false;
	}
	
	private boolean addAsXorSplit(Transition trans) {	
		StateFormula cond1 = this.getCorrespondingCondition(trans);
		State source = trans.getSource();
		
		if (!cond1.isEmpty()) {
			boolean thereIsAnotherBranch = false;
			
			for (Transition t2 : this.simplifiedSTS.getTransitionsFromState(trans.getSource())) {
				if (!t2.getCondition().equals(trans.getCondition()) || !t2.getTarget().equals(trans.getTarget())) {
					thereIsAnotherBranch = true;
				}
			}
			if (!thereIsAnotherBranch) {
				return false;
			}
			
			trans2cond.put(trans, cond1);
			ProcessNode node = null;
			for (Transition t2 : trans2cond.keySet()) {
				if (!trans2node.containsKey(t2)) {
					continue;
				}
				StateFormula cond2 = trans2cond.get(t2);				
				if (t2.getSource().equals(source) && !cond1.equals(cond2)) {
					node = trans2node.get(t2);
					break;
				}
			}
			if (node == null) {
				node = new XorSplit();
				this.newPM.addVertex(node);
			}
			trans2node.put(trans, node);
			return true;
		}
		return false;
	}
	
	
	private void addEdges() {		
		for (Transition trans : simplifiedSTS.getTransitions()) {
			if (trans.getActionName() == "" || trans.getActionName().equals(defaultName)) {
				continue;
			}
			State toState = trans.getTarget();
			Set<Transition> connections = new HashSet<Transition>();
			for (Transition outT : simplifiedSTS.getTransitionsFromState(toState)) {
				connections.addAll(collectNextTransitions(outT));
			}
			for (Transition conn : connections) {
				logger.trace("Adding edge for \n   " + trans.toString() + 
						"\n   and " + conn.toString());
				connectProcessNodes(trans, conn);
			}
		}
	}
	
	private Set<Transition> collectNextTransitions(Transition trans) {
		Set<Transition> nextTrans = new HashSet<Transition>();
		
		if (trans.getActionName() != "" && !trans.getActionName().equals(defaultName)) {
			nextTrans.add(trans);
		} else {
			State toState = trans.getTarget();
			for (Transition outT : simplifiedSTS.getTransitionsFromState(toState)) {
				nextTrans.addAll(collectNextTransitions(outT));
			}
		}
		return nextTrans;
	}

	private void connectProcessNodes(Transition incomingTrans, 
			Transition outgoingTrans) {
		ProcessNode node1 = trans2node.get(incomingTrans);
		ProcessNode node2 = trans2node.get(outgoingTrans);
		logger.trace("Connecting " + incomingTrans + "\nwith " + outgoingTrans);
		
		ProcessEdge edge = newPM.addEdge(node1, node2);
		if (edge == null) {
			edge = newPM.getEdge(node1, node2);
		}
		StateFormula cond = trans2cond.get(incomingTrans);
		if (cond == null) {
			cond = new StateFormula();
		}
		edge.setCondition(cond);
		logger.trace("Added edge: " + edge);
	}

	
	private void addXorJoinNodes() {
		Set<ProcessEdge> totalInEdges = new HashSet<ProcessEdge>();
		Set<ProcessNode> originalNodes = new HashSet<ProcessNode>(
				this.newPM.vertexSet());

		for (ProcessNode node : originalNodes) {
			if (newPM.inDegreeOf(node) > 1) {
				Set<ProcessEdge> inEdges = new HashSet<ProcessEdge>(
						newPM.incomingEdgesOf(node));
				totalInEdges.addAll(inEdges);
				
				XorJoin xorJoin = new XorJoin();
				newPM.addVertex(xorJoin);
				
				for (ProcessEdge edge : inEdges) {
					ProcessNode source = newPM.getEdgeSource(edge);
					ProcessEdge newEdge = newPM.addEdge(source, xorJoin);
					if (newEdge != null) {
						newEdge.setCondition(edge.getCondition());
					}
				}
				newPM.removeAllEdges(inEdges);
				newPM.addEdge(xorJoin, node);
			}
		}
	}
	
	private void addXorSplitNodes() {
		Set<ProcessEdge> totalOutEdges = new HashSet<ProcessEdge>();
		Set<ProcessNode> originalNodes = new HashSet<ProcessNode>(
				this.newPM.vertexSet());

		for (ProcessNode node : originalNodes) {
			if (newPM.outDegreeOf(node) > 1 && !(node instanceof XorSplit)) {
				Set<ProcessEdge> outEdges = new HashSet<ProcessEdge>(
						newPM.outgoingEdgesOf(node));
				totalOutEdges.addAll(outEdges);
				
				XorSplit xorSplit = new XorSplit();
				newPM.addVertex(xorSplit);
				
				for (ProcessEdge edge : outEdges) {
					ProcessNode target = newPM.getEdgeTarget(edge);
					ProcessEdge newEdge = newPM.addEdge(xorSplit, target);
					if (newEdge != null) {
						newEdge.setCondition(edge.getCondition());
					}
				}
				newPM.removeAllEdges(outEdges);
				newPM.addEdge(node, xorSplit);
			}
		}
	}
	
	private Activity getCorrespondingActivity(Transition trans) {
		String actionName = getActionName(trans);
//		logger.info("Looking for " + actionName);
		
		Activity activity = null;
		for (Action originalAct : action2activity.keySet()) {
			if (actionName.equals(originalAct.getName())) {
				activity = action2activity.get(originalAct);
				break;
			}
		}
		return activity;
	}

	private StateFormula getCorrespondingCondition(Transition trans) {
		String actionName = getActionName(trans);		
		logger.trace("Looking for condition of " + actionName);
		
		StateFormula cond = new StateFormula();
		
		for (Action originalAct : this.action2condition.keySet()) {
			if (actionName.equals(originalAct.getName())) {
				cond.add(action2condition.get(originalAct).getFormula());
				logger.trace("Found " + cond);
				break;
			}
		}
		return cond;
	}
	
	private String getActionName(Transition trans) {
		String actionName = trans.getActionName();
		
		if (actionName.endsWith(" &\\\\n")) {
			actionName = actionName.substring(0, actionName.length()-5);
		}
	
		String[] tokens = actionName.split(" = ");
		if (tokens.length > 1) {
			actionName = tokens[1];
		} 
		
		return actionName;
	}


	public STS simplifySTS(STS inputSTS) {
		STS stsCopy = inputSTS.getCopy();
		boolean updated = false;

		List<Transition> invisibleTransitions = new ArrayList<Transition>();
		Set<Transition> toRemove = new HashSet<Transition>();

		for (Transition t : stsCopy.getTransitions()) {
			String actName = t.getActionName();
			
			if (actName.isEmpty() || actName.contains("Trigger") || actName.equals(defaultName)) {
				boolean alreadyAdded = false;
				boolean isAnAlternative = false;
				boolean isLast = false;
				
				for (Transition t2 : invisibleTransitions) {
					if (t.equals(t2)) {
						alreadyAdded = true;
						break;
					}
					if (t.getSource().equals(t2.getSource()) && t.getTarget().equals(t2.getTarget())) {
						toRemove.add(t);
						isAnAlternative = true;
						updated = true;
						break;
					}
				}
				if (stsCopy.getTransitionsFromState(t.getTarget()).isEmpty()) {
					isLast = true;
					toRemove.add(t);
					updated = true;
				}
				if (!alreadyAdded && !isAnAlternative && !isLast) {
						invisibleTransitions.add(t);
				}
			}
		}
		
		for (Transition t : toRemove) {
			stsCopy.removeTransition(t);
//			logger.info("Removing alternative " + t);
		}
		stsCopy.refreshStates();
		
		while (!invisibleTransitions.isEmpty()) {			
			Transition t = invisibleTransitions.remove(0);
			logger.trace("Looking at " + t);
			
			if (stsCopy.hasAlternativePath(t)) {
				logger.trace(".. has alternative path");
				continue;
			}
			if (stsCopy.getTransitionsFromState(t.getTarget()).size() > 1 && 
					stsCopy.getTransitionsToState(t.getSource()).size() > 1) {
				continue;
			}
			
			updated = true;
			stsCopy.removeTransition(t);
			stsCopy.replaceState(t.getTarget(), t.getSource());
			logger.trace(".. removing transition");
			
			for (Transition t2 : invisibleTransitions) {
				t2.replaceState(t.getTarget(), t.getSource());
			}
		}

		stsCopy.removeUnusedActions();
//		stsCopy.removeUnusedStates();
		stsCopy.refreshStates();
		
		if (updated) {
			removeDuplicateTransitions(stsCopy);
			return simplifySTS(stsCopy);
		}
		
		return stsCopy;
	}


	private void removeDuplicateTransitions(STS stsCopy) {
		Set<Transition> toRemove = new HashSet<Transition>();
		
		for (Transition t1 : stsCopy.getTransitions()) {
			if (toRemove.contains(t1) || t1.getActionName().isEmpty() 
					|| t1.getActionName().equals(defaultName)) {
				continue;
			}
			for (Transition t2 : stsCopy.getTransitions()) {
				if (t1.equals(t2) || toRemove.contains(t2) || t2.getActionName().isEmpty()) {
					continue;
				}
				if (t1.getSource().equals(t2.getSource()) 
						&& t1.getTarget().equals(t2.getTarget()) 
						&& t1.getActionName().equals(t2.getActionName())) {
					toRemove.add(t2);
				}
			}
		}
		for (Transition trans : toRemove) {
			stsCopy.removeTransition(trans);
		}
		
	}

	public STS getSimplifiedSTS() {
		return this.simplifiedSTS;
	}
	
}
