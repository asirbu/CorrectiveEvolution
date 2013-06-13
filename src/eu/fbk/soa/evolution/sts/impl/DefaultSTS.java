package eu.fbk.soa.evolution.sts.impl;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import eu.fbk.soa.evolution.sts.Action;
import eu.fbk.soa.evolution.sts.STS;
import eu.fbk.soa.evolution.sts.State;
import eu.fbk.soa.evolution.sts.Transition;



public class DefaultSTS extends AbstractSTS implements STS {

	private Set<Action> inputActions = new HashSet<Action>();

	private Set<Action> outputActions = new HashSet<Action>();
	
	private Map<String, Set<State>> labels = new HashMap<String, Set<State>>();

//	private String name;

	public DefaultSTS(Set<State> states, State initialState, Set<Action> inputActions,
			Set<Action> outputActions, Set<Transition> transitions) {

		this.states = states;
		this.initialState = initialState;
		this.inputActions = inputActions;
		this.outputActions = outputActions;
		this.transitions = transitions;
	}

	public DefaultSTS() {
		this.states = new HashSet<State>();
		this.inputActions = new HashSet<Action>();
		this.outputActions = new HashSet<Action>();
		this.transitions = new HashSet<Transition>();
	}
	
	public boolean isEmpty() {
		return (states.isEmpty() && initialState == null 
				&& inputActions.isEmpty() && outputActions.isEmpty() 
				&& transitions.isEmpty());
	}


	
	@Override
	public State getInitialState() {
		return initialState;
	}

	public Set<Action> getInputActions() {
		return inputActions;
	}

	public Set<Action> getOutputActions() {
		return outputActions;
	}

	
	@Override
	public void setInitialState(State state) {
		initialState = state;
	}

	public void addInputAction(Action action) {
		inputActions.add(action);
	}

	public void addOutputAction(Action action) {
		outputActions.add(action);
	}

	@Override
	public void addAction(Action action) {
		if (action.isInputAction()) {
			inputActions.add(action);
		} else {
			outputActions.add(action);
		}

	}

	

	@Override
	public void addActions(Collection<Action> actions) {
		for (Action act : actions) {
			this.addAction(act);
		}
	}

	public Action searchAction(Set<Action> actions, String actionName) {
		for (Action action : actions) {
			if (action.getName().equals(actionName)) {
				return action;
			}
		}
		return null;
	}
	
	@Override
	public Set<Action> getActions() {
		Set<Action> allActions = new HashSet<Action>(inputActions);
		allActions.addAll(outputActions);
		return allActions;
	}

	@Override
	public void labelState(State state, String label) {
		Set<State> states = this.labels.get(label);
		if (states == null) {
			states = new HashSet<State>();
		}
		states.add(state);
		labels.put(label, states);
	}
	
	@Override
	public Set<State> getStatesForLabel(String label) {
		Set<State> litStates = this.labels.get(label);
		if (litStates == null) {
			litStates = new HashSet<State>();
		}
		return litStates;
	}

	@Override
	public Action getAction(String string) {
		for (Action act : this.inputActions) {
			if (act.getName().equals(string)) {
				return act;
			}
		}
		for (Action act : this.outputActions) {
			if (act.getName().equals(string)) {
				return act;
			}
		}
		return null;
	}

	@Override
	public STS getCopy() {
		STS cloneSTS = new DefaultSTS();
		cloneSTS.addStates(this.states);
		cloneSTS.setInitialState(this.initialState);
		cloneSTS.addActions(inputActions);
		cloneSTS.addActions(outputActions);
		cloneSTS.addTransitions(this.transitions);
		return cloneSTS;
		
	}
	
	@Override
	public void removeAction(Action action) {
		if (action.isInputAction()) {
			this.inputActions.remove(action);
		} else {
			this.outputActions.remove(action);
		}
	}

}
