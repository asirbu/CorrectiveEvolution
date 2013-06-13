package eu.fbk.soa.evolution.sts.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import eu.fbk.soa.evolution.sts.Action;
import eu.fbk.soa.evolution.sts.STS;
import eu.fbk.soa.evolution.sts.State;
import eu.fbk.soa.evolution.sts.Transition;



public abstract class AbstractSTS implements STS {
	
	static Logger logger = Logger.getLogger(AbstractSTS.class);

	Set<State> states;
	
	State initialState;
	
	Set<Transition> transitions;
	
	private String name;
	
	@Override
	public void addState(State state) {
		states.add(state);
	}
	
	@Override
	public void addInitialState(State state) {
		initialState = state;
		states.add(state);
	}
	
	@Override
	public void addStates(Collection<State> newStates) {
		this.states.addAll(newStates);
	}

	@Override
	public void addTransition(Transition transition) {
		transitions.add(transition);
	}
	
	@Override
	public void addTransitions(Collection<Transition> transitions) {
		for (Transition t : transitions) {
			this.addTransition(t);
		}
	}
	
	@Override
	public Set<State> getFinalStates() {
		Set<State> finalStates = new HashSet<State>();
		for (State state : this.getStates()) {
			if (getTransitionsFromState(state).isEmpty()) {
				finalStates.add(state);
			}
		}
		return finalStates;
	}
	
	@Override
	public State getState(String stateName) {
		for (State state : states) {
			if (state.getName().equals(stateName)) {
				return state;
			}
		}
		return null;
	}
	
	@Override
	public Set<State> getStates() {
		return states;
	}
	
	@Override
	public Set<State> getStatesForLabel(String label) {
		return new HashSet<State>();
	}
	
	@Override
	public Set<Transition> getTransitions() {
		return this.transitions; 
	}
	
	@Override
	public Set<Transition> getTransitionsFromState(State state) {
//		if (!states.contains(state)) {
//			throw new IllegalArgumentException("STS " +
//					"does not contain state " + state);
//		}	
		Set<Transition> transFromState = new HashSet<Transition>();
		for (Transition t : this.transitions) {
			if (t.getSource().equals(state)) {
				transFromState.add(t);
			}
		}
		return transFromState;
	}
	
	@Override
	public Set<Transition> getTransitionsToState(State state) {
//		if (!states.contains(state)) {
//			throw new IllegalArgumentException("STS " +
//					"does not contain state " + state);
//		}	
		Set<Transition> transToState = new HashSet<Transition>();
		for (Transition t : this.transitions) {
			if (t.getTarget() == state) {
				transToState.add(t);
			}
		}
		return transToState;
	}
	
	@Override
	public boolean hasAlternativePath(Transition trans) {
//		logger.info("Checking for alternative path to " + trans);
		List<Transition> otherTransitions = new ArrayList<Transition>(
				this.getTransitionsFromState(trans.getSource()));
		otherTransitions.remove(trans);
		Set<Transition> visitedTransitions = new HashSet<Transition>();
		visitedTransitions.add(trans);
		
		while (!otherTransitions.isEmpty()) {
			Transition otherTrans = otherTransitions.remove(0);
			if (otherTrans.getTarget().equals(trans.getTarget())) {
				logger.trace("End of alternative path: " + otherTrans);
				return true;
			}
			visitedTransitions.add(otherTrans);
			
			Set<Transition> nextTransitions = new HashSet<Transition>(
					this.getTransitionsFromState(otherTrans.getTarget()));
			nextTransitions.removeAll(visitedTransitions);
			nextTransitions.removeAll(otherTransitions);
			
			otherTransitions.addAll(nextTransitions);
		}
		return false;
	}
	
	@Override
	public void labelState(State state, String label) {
	}

	@Override
	public boolean removeState(State state) {
		return this.states.remove(state);
		
	}

	@Override
	public boolean removeTransition(Transition trans) {
		Transition toRemove = null;
		for (Transition t : this.transitions) {
			if (t.equals(trans)) {
				toRemove = t;
				break;
			}
		}
		if (toRemove != null) {
			return this.transitions.remove(toRemove);
		}
		return false;
//		return this.transitions.remove(trans);
	}
	
	@Override 
	public void removeUnusedActions() {
		Set<Action> actionsCopy = new HashSet<Action>(this.getActions());
		for (Action a : actionsCopy) {
			boolean notUsed = true;
			for (Transition t : transitions) {
				if (t.getAction().equals(a)) {
					notUsed = false;
					break;
				}
			}
			if (notUsed) {
				this.removeAction(a);
			}
		}
	}
	
	@Override
	public void removeUnusedStates() {
		Set<State> statesCopy = new HashSet<State>(this.states);
		for (State state : statesCopy) {
			boolean notUsed = true;
			for (Transition t : transitions) {
				if (t.getSource().equals(state) || t.getTarget().equals(state)) {
					notUsed = false;
					break;
				}
			}
			if (notUsed) {
				this.states.remove(state);
			}
		}
	}
	
	@Override
	public void refreshStates() {
		Set<State> currentStates = new HashSet<State>();
		for (Transition t : transitions) {
			currentStates.add(t.getSource());
			currentStates.add(t.getTarget());
		}
		states = currentStates;
	}
	
	public void replaceState(State state, State replacement) {
		states.remove(state); 
		Set<Transition> transitionsCopy = new HashSet<Transition>(transitions);
		for (Transition t : transitionsCopy) {
			Transition newTrans = t.replaceState(state, replacement);
			transitions.remove(t);
			transitions.add(newTrans);
		}
	}
	
	public void setInitialState(String stateName) {
		for (State state : states) {
			if (state.getName().equals(stateName)) {
				initialState = state;
			}
		}
	}

	
	@Override 
	public void setName(String name) {
		this.name = name;
	}

	public String toDot() {
		String dotSpec = "digraph sts { \n" +
//			"ratio=\"1.3\";\n margin=\"0.2\";\n size=\"8.0,11.4\";\n edge[fontsize=24,penwidth=2];\n";
			"center = TRUE; \n mclimit = 10.0; \n nodesep = 0.05; \n node [width=0.25, height=0.25];\n";
		for (Transition t : transitions) {
			dotSpec += t.toDot() + "\n";
		}
		dotSpec += "}";
		return dotSpec;
	}
	
	public String toString() {
		String string = "STS\n States: ";
		for (State s : states) {
			string += s.getName() + " ";
		}
		if (this.getInitialState() != null) {
			string += "\n Initial state: " + this.getInitialState().getName();
		}
		string += "\n Input actions: ";
		for (Action a : this.getInputActions()) {
			string += a.getName() + " ";
		}
		string += "\n Output actions: ";
		for (Action a : this.getOutputActions()) {
			string += a.getName() + " ";
		}
		string += "\n Transitions:\n";
		for (Transition t : transitions) {
			string += "   " + t.toString() + "\n";
		}
		return string;
	}
	
}
