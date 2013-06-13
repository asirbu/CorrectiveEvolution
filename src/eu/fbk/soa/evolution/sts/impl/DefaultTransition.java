package eu.fbk.soa.evolution.sts.impl;

import java.util.HashSet;
import java.util.Set;

import eu.fbk.soa.evolution.sts.Action;
import eu.fbk.soa.evolution.sts.Clause;
import eu.fbk.soa.evolution.sts.Literal;
import eu.fbk.soa.evolution.sts.State;
import eu.fbk.soa.evolution.sts.Transition;
import eu.fbk.soa.process.StateFormula;


public class DefaultTransition implements Transition {

	private State beginState;

	private StateFormula condition;

	private Action action;

	private State endState;
	
	private Set<Clause<? extends Literal>> guardClauses;

	public DefaultTransition(State beginState, StateFormula condition, Action action,
			State endState) {

		this.beginState = beginState;
		this.condition = condition;
		this.action = action;
		this.endState = endState;
		this.guardClauses = new HashSet<Clause<? extends Literal>>();
	}

	public DefaultTransition(State beginState, Action action, State endState) {
		this(beginState, new StateFormula(), action, endState);
	}
	
	
	@Override
	public State getSource() {
		return beginState;
	}

	public StateFormula getCondition() {
		return condition;
	}

	@Override
	public Action getAction() {
		return action;
	}

	@Override
	public State getTarget() {
		return endState;
	}

	public String toString() {
		String str = "Transition from State " + beginState.getName() +
				" to State " + endState.getName() + " through Action " + action.getName();
		return str;
//		return "<" + beginState + ", " + condition.toString() + ", " + action
//				+ ", " + endState + ">";
	}

	@Override
	public DefaultTransition replaceState(State state, State replacement) {
		State newFromState = beginState.equals(state)? replacement: beginState;
		State newToState = endState.equals(state)? replacement: endState;
		return new DefaultTransition(newFromState, this.condition, this.action, newToState); 	
	}
	
	public int hashCode() {
//		int hashcode = beginState.hashCode() + condition.hashCode() +
//			action.hashCode() + endState.hashCode();
		return 0;
	}
	
	public boolean equals(Object obj) {
		if (!(obj instanceof DefaultTransition)) {
			return false;
		}
		
		DefaultTransition t = (DefaultTransition) obj;
		if ((t.getSource().equals(this.beginState)) &&
			(t.getTarget().equals(this.endState)) &&
			(t.getCondition().equals(this.condition)) &&
			(t.getAction().equals(this.action))) {
			return true;
		}
		return false;
	}
	
	@Override
	public String toDot() {
		String dotSpec = "\"" + beginState.getName() + "\" -> \"" + endState.getName() + "\" ";
		dotSpec += "[ label = \"" + action.getName() + "\"]";
		return dotSpec;
	}

	@Override
	public void setCondition(StateFormula cond) {
		this.condition = cond;
		
	}


	@Override
	public Set<Clause<? extends Literal>> getGuardClauses() {
		return guardClauses;
	}

	@Override
	public void addGuardClause(Clause<? extends Literal> clause) {
		this.guardClauses.add(clause);
	}

	@Override
	public void addGuardClauses(Set<Clause<? extends Literal>> clauses) {
		this.guardClauses.addAll(clauses);
	}

	@Override
	public String getActionName() {
		if (this.action != null) {
			return this.action.getName();
		}
		return "";
	}
	
}
