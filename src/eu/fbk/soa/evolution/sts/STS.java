package eu.fbk.soa.evolution.sts;

import java.util.Collection;
import java.util.Set;

public interface STS {

	public void addState(State state);
	
	public void addStates(Collection<State> newStates);

	public void addInitialState(State state);

	public void addTransition(Transition transition);

	public void addAction(Action action);

	public void addActions(Collection<Action> actions);

	public State getInitialState();

	public Set<State> getStates();
	     
	public Set<Action> getActions();
	
	public Set<Transition> getTransitions();
	
	public Set<Transition> getTransitionsFromState(State state);

	public Set<Action> getInputActions();
	
	public Set<Action> getOutputActions();

	public Set<State> getFinalStates();

	public State getState(String string);

	public void labelState(State state, String label);

	public Set<State> getStatesForLabel(String label);

	public void addTransitions(Collection<Transition> transitions);

	public Action getAction(String string);

	public void setName(String name);

	public void setInitialState(State state);
	
	public void setInitialState(String stateName);

	public String toDot();

	public STS getCopy();

	public void replaceState(State state, State replacement);

	public Set<Transition> getTransitionsToState(State state);

	public boolean removeTransition(Transition trans);

	public boolean removeState(State state);

	public void removeUnusedActions();

	public void removeAction(Action action);

	public void removeUnusedStates();

	public boolean hasAlternativePath(Transition trans);

	void refreshStates();

}