package eu.fbk.soa.evolution.sts.impl;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import eu.fbk.soa.evolution.sts.Action;
import eu.fbk.soa.evolution.sts.STS;
import eu.fbk.soa.evolution.sts.State;
import eu.fbk.soa.evolution.sts.Transition;




public class NusmvSTS extends AbstractSTS implements STS {
	
	private Set<Action> actions;
	
	public NusmvSTS() {
		this.states = new HashSet<State>();
		this.actions = new HashSet<Action>();
		this.transitions = new HashSet<Transition>();
	}

	public NusmvState addState(Map<String, String> varValues) {		
		NusmvState newState = new NusmvState(varValues);
		for (State state : states) {
			if (state.equals(newState))
				return (NusmvState) state;
		}
		this.states.add(newState);
		return newState;
	}

	public Action addAction(Map<String, String> varValues) {
		Action newAction = new NusmvAction(varValues);
		for (Action act : actions) {
			if (act.equals(newAction))
				return act;
		}
		this.actions.add(newAction);
		return newAction;
	}
	
	public State getInitialState() {
		for (State state : states) {
			boolean isInitial = true;
			for (Transition t : transitions) {
				if (t.getTarget().equals(state)) {
					isInitial = false;
					break;
				}
			}
			if (isInitial) {
				return state;
			}
		}
		return null;
	}

	public Set<Action> getActions() {
		return this.actions;
	}
	
	public NusmvSTS getCopy() {
		NusmvSTS cloneSTS = new NusmvSTS();
		cloneSTS.addStates(this.states);
		cloneSTS.addTransitions(this.transitions);
		cloneSTS.addActions(this.actions);
		return cloneSTS;
	}

	public void addActions(Collection<Action> actionCollection) {
		this.actions.addAll(actionCollection);		
	}

	@Override
	public void addAction(Action newAction) {
		for (Action act : actions) {
			if (act.equals(newAction)) {
				return;
			}
		}
		this.actions.add(newAction);
	}

	@Override
	public Set<Action> getInputActions() {
		Set<Action> inputActions = new HashSet<Action>();
		for (Action act : this.actions) {
			if (act.isInputAction()) {
				inputActions.add(act);
			}
		}
		return inputActions;
	}

	@Override
	public Set<Action> getOutputActions() {
		Set<Action> outputActions = new HashSet<Action>();
		for (Action act : this.actions) {
			if (!act.isInputAction()) {
				outputActions.add(act);
			}
		}
		return outputActions;
	}
	
	
	@Override
	public void setInitialState(State state) {
	}
	
	@Override
	public Action getAction(String string) {
		for (Action act : this.actions) {
			if (act.getName().equals(string)) {
				return act;
			}
		}
		return null;
	}

	@Override
	public void removeAction(Action action) {
		this.actions.remove(action);	
	}


}
