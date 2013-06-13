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
import eu.fbk.soa.evolution.sts.impl.DefaultSTS;
import eu.fbk.soa.evolution.sts.impl.DefaultState;
import eu.fbk.soa.evolution.sts.impl.DefaultTransition;
import eu.fbk.soa.process.Activity;
import eu.fbk.soa.process.ProcessEdge;
import eu.fbk.soa.process.ProcessModel;
import eu.fbk.soa.process.StateFormula;
import eu.fbk.soa.process.node.ActivityNode;
import eu.fbk.soa.process.node.AndJoin;
import eu.fbk.soa.process.node.AndSplit;
import eu.fbk.soa.process.node.ProcessNode;
import eu.fbk.soa.process.node.XorJoin;
import eu.fbk.soa.process.node.XorSplit;
import eu.fbk.soa.util.ArrayUtils;

public class ProcessModelToSTS {

	private static Logger logger = Logger.getLogger(ProcessModelToSTS.class);
	
	private int stateIndex;
	
	private static int xorCount = 0;
	
	private static int andCount = 0;
	
	private Set<State> states;

	private Set<Action> inputActions;

	private Set<Action> outputActions;

	private Set<Transition> transitions;

	private Map<XorJoin, State> xorJoinState;
	
	private Map<ActivityNode, Transition> correspondences;
	
	private Map<Action, Activity> actionTable;

	
	public ProcessModelToSTS() {
		this.correspondences = new HashMap<ActivityNode, Transition>();
		this.actionTable = new HashMap<Action, Activity>();
	}
	
	public STS transformProcessModel(ProcessModel model) {
		ProcessNode start = model.getStartNode();
		return transformProcessModel(model, start);
	}
	
	public STS transformProcessModel(ProcessModel model, ProcessNode start) {
		init();
		State initialState = createState();			
		transform(start, initialState, model);
		return new DefaultSTS(states, initialState, inputActions, 
				outputActions, transitions);	
	}
	
	private void init() {
		this.stateIndex = 0;
		this.states = new HashSet<State>();
		xorJoinState = new HashMap<XorJoin, State>();
		
		inputActions = new HashSet<Action>();
		outputActions = new HashSet<Action>();
		transitions = new HashSet<Transition>();
	}
		
	private State createState() {
		return createState("s" + stateIndex);
	}
	
	private State createState(String name) {
		State newState = new DefaultState(name);
		stateIndex++;
		states.add(newState);
		return newState;
	}
	
	private void transform(ProcessNode node, State fromState, ProcessModel model) {
		if (node instanceof ActivityNode) {
			transformActivityNode((ActivityNode) node, fromState, model);
		} 
		if (node instanceof XorSplit) {
			transformXorSplit((XorSplit) node, fromState, model);
		}
		if (node instanceof XorJoin) {
			transformXorJoin((XorJoin) node, fromState, model);
		}
		if (node instanceof AndSplit) {
			transformAndSplit((AndSplit) node, fromState, model);
		}
	}
	
	private void transformActivityNode(ActivityNode actNode, State fromState, ProcessModel model) {
		State nextState = transformActivity(actNode, fromState, model);
		for (ProcessEdge edge : model.outgoingEdgesOf(actNode)) {
			transform(model.getEdgeTarget(edge), nextState, model);
		}
	}
	
	private void transformXorSplit(XorSplit splitNode, State fromState, ProcessModel model) {
		State nextState = createState();
		
		xorCount++;
		Action action = new DefaultAction("Xor" + xorCount, true, model);
		Transition trans = addActionToSTS(action, fromState, nextState, new StateFormula());
		int i = 1;
		//TODO if a branch has no condition, negate and conjoin the conditions on the other branches
		
		Map<ProcessNode, State> nextNodes = new HashMap<ProcessNode, State>();
		for (ProcessEdge edge : model.outgoingEdgesOf(splitNode)) {
			State branchState = createState();
			Action caseAction = new DefaultAction("Case" + xorCount + i, false, model);
			i++;
			StateFormula condition = edge.getCondition();
			if (condition.isEmpty()) {
				for (ProcessEdge otherEdge : model.outgoingEdgesOf(splitNode)) {
					if (!otherEdge.equals(edge) && !otherEdge.getCondition().isEmpty()) {
						condition.add(otherEdge.getCondition().getNegation());
					}
				}
			}
			addActionToSTS(caseAction, nextState, branchState, condition);
			
			logger.trace("Adding sts action for XOR edge with condition " + condition);
			nextNodes.put(model.getEdgeTarget(edge), branchState);
		}

		for (ProcessNode nextNode : nextNodes.keySet()) {
			transform(nextNode, nextNodes.get(nextNode), model);
		}
	}
	
	private void transformXorJoin(XorJoin joinNode, State fromState, ProcessModel model) {
		State state = xorJoinState.get(joinNode);
		if (state != null) {
			replaceState(fromState, state);
		} else {
			xorJoinState.put(joinNode, fromState);
			Set<ProcessEdge> edges = model.outgoingEdgesOf(joinNode);
			for (ProcessEdge edge : edges) {
				transform(model.getEdgeTarget(edge), fromState, model);
			}
		}
	}
	
	private void replaceState(State state, State replacement) {
		if (states.remove(state)) {
			if (this.xorJoinState.containsValue(state)) {
				Set<XorJoin> keys = xorJoinState.keySet();
				for (XorJoin key : keys) {
					if (xorJoinState.get(key).equals(state)) {
						xorJoinState.put(key, replacement);
					}
				}
			}
			Set<Transition> transitionsCopy = new HashSet<Transition>(transitions);
			for (Transition t : transitionsCopy) {
				Transition newTrans = t.replaceState(state, replacement);
				transitions.remove(t);
				transitions.add(newTrans);
				
				ActivityNode correspNode = null;
				for (ActivityNode node : correspondences.keySet()) {
					if (correspondences.get(node).equals(t)) {
						correspNode = node;
						break;
					}
				}
				if (correspNode != null) {
					correspondences.put(correspNode, newTrans);
				}
			}
		}
	}
	
	private void transformAndSplit(AndSplit node, State fromState, ProcessModel model) {
		State nextState = createState();
		
		andCount++;
		Action action = new DefaultAction("And" + andCount, true, model);
		addActionToSTS(action, fromState, nextState, new StateFormula());
	
//		if (model.outDegreeOf(node) > 2) {
//			throw new UnsupportedOperationException("AND blocks can only have two branches");
//		}
		
		List<ActivityNode> acts = new ArrayList<ActivityNode>();
		List<State> branchStates = new ArrayList<State>();
		ProcessNode join = null;
		
		for (ProcessEdge edge : model.outgoingEdgesOf(node)) {
			
			ProcessNode nextNode = model.getEdgeTarget(edge);
			Set<ProcessEdge> nextEdges = model.outgoingEdgesOf(nextNode);
			if (!(nextNode instanceof ActivityNode) || nextEdges.size() != 1) {
				throw new UnsupportedOperationException("AND blocks can contain only simple activities");
			}
			acts.add((ActivityNode) nextNode);
			
			ProcessEdge nextEdge = nextEdges.iterator().next();
			ProcessNode target = model.getEdgeTarget(nextEdge);
			if (join == null) {
				join = target;
			}
			if (!(target instanceof AndJoin) || !target.equals(join)) {
				throw new UnsupportedOperationException("AND blocks can contain only simple activities");				
			}	
		}
		
		ArrayUtils<ActivityNode> arrayUtils = new ArrayUtils<ActivityNode>();
		List<List<ActivityNode>> nodeOrders = arrayUtils.computePermutations(acts);
		
//		int branch = 0;
		int i = 0;
		List<State> finalStates = new ArrayList<State>();
		
		for (List<ActivityNode> nodeOrder : nodeOrders) {
			State branchState = createState();
			branchStates.add(branchState);
			Action orderAction = new DefaultAction("Order" + andCount + i, false, model);
			i++;
			addActionToSTS(orderAction, nextState, branchState, StateFormula.getTop());
			
			State currentState = branchState;
			for (ActivityNode actNode : nodeOrder) {
				State intermState = transformActivity(actNode, currentState, model);
				currentState = intermState;
			}
			finalStates.add(currentState);
		}
		
//		State intermState1 = transformActivity(acts.get(0), branchStates.get(0), model);
//		State finalState1 = transformActivity(acts.get(1), intermState1, model);
//		
//		State intermState2 = transformActivity(acts.get(1), branchStates.get(1), model);
//		State finalState2 = transformActivity(acts.get(0), intermState2, model);
		for (int j = 1; j < finalStates.size(); j++) {
			replaceState(finalStates.get(j), finalStates.get(0));	
		}
			
		for (ProcessEdge edge : model.outgoingEdgesOf(join)) {
			transform(model.getEdgeTarget(edge), finalStates.get(0), model);
		}
			
	}
	
	private Transition addActionToSTS(Action action, State startState, State endState, 
			StateFormula cond) {
		
		if (action.isInputAction()) {
			inputActions.add(action);
		} else {
			outputActions.add(action);
		}
		Transition trans = createTransition(startState, cond, action, endState);
		return trans;
	}
	
	private State transformActivity(ActivityNode actNode, State fromState, ProcessModel model) {
		State endState = createState();
		
		Activity act = actNode.getActivity();
		logger.trace("Transforming activity " + act);
		
		String actionName = createActionName(act);
		Action action = null;
		for (Action inputAct : inputActions) {
			if (inputAct.getName().equals(actionName) && 
					inputAct.getRelatedEntity().equals(model)) {
				action = inputAct;
			}
		}
		if (action == null) {
			action = new DefaultAction(actionName, true, act, model);
		}
		if (action != null) {
			Transition trans = addActionToSTS(action, fromState, endState, act.getPrecondition());
			logger.trace("Created " + trans + " for activity node " + actNode.getNodeID() + "(" + act.getName() + ")");
			correspondences.put(actNode, trans);
			actionTable.put(action, act);
		}		
		return endState;
	}
	
	private Transition createTransition(State beginState, StateFormula cond, 
			Action action, State endState) {
		Transition trans = new DefaultTransition(beginState, cond, action, endState);
		transitions.add(trans);
		return trans;
	}
	
	private String createActionName(Activity act) {
		return act.getName().replace(" ", "_"); 
	}

	public Transition getCorrespondingTransition(ActivityNode node) {
		return this.correspondences.get(node);
	}
	
	public Map<Action, Activity> getActionTable() {
		return this.actionTable;
	}	
}
