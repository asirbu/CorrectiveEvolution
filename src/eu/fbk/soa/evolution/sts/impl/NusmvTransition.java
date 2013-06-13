package eu.fbk.soa.evolution.sts.impl;

import java.util.HashSet;
import java.util.Set;

import eu.fbk.soa.evolution.sts.Action;
import eu.fbk.soa.evolution.sts.Clause;
import eu.fbk.soa.evolution.sts.Literal;
import eu.fbk.soa.evolution.sts.State;
import eu.fbk.soa.evolution.sts.Transition;
import eu.fbk.soa.process.StateFormula;



public class NusmvTransition implements Transition {

	private State fromState;
	
	private State toState;
	
	private Action action;
	
	public NusmvTransition(State beginState, Action action, State endState) {
		this.fromState = beginState;
		this.action = action;
		this.toState = endState;
	}

	public String toDot() {
		String dotSpec = "\"" + fromState.getName() + "\" -> \"" + toState.getName() + "\" ";
		dotSpec += "[ label = \"" + action.getName() + "\"]";
		return dotSpec;
	}
	
	public boolean equals(Object obj) {
		if (!(obj instanceof NusmvTransition)) {
			return false;
		}
		
		NusmvTransition t = (NusmvTransition) obj;
		boolean comparison = ((t.getSource().equals(this.fromState)) &&
			(t.getTarget().equals(this.toState)) &&
			(t.getAction().equals(this.action)));
		
//		System.out.println("Comparing " + this.toString() + 
//				" with " + t.toString() + ": " + comparison);
		
		return comparison;
	}
	
	public int hashCode() {
		int hashcode = fromState.hashCode() + 
			action.hashCode() + toState.hashCode();
		return hashcode;
	}

	public Action getAction() {
		return this.action;
	}

	public State getTarget() {
		return toState;
	}

	public State getSource() {
		return this.fromState;
	}
	
	public String toString() {
		String str = "Transition from State " + fromState.getName() +
			" to State " + toState.getName() + " through Action " + action.getName();
		return str;
	}

	@Override
	public Transition replaceState(State state, State replacement) {
		State newFromState = fromState.equals(state)? replacement: fromState;
		State newToState = toState.equals(state)? replacement: toState;
		return new NusmvTransition(newFromState, action, newToState); 	
	}

	@Override
	public StateFormula getCondition() {
		return new StateFormula();
	}

	@Override
	public void setCondition(StateFormula cond) { }


	@Override
	public Set<Clause<? extends Literal>> getGuardClauses() {
		return new HashSet<Clause<? extends Literal>>();
	}

	@Override
	public void addGuardClause(Clause<? extends Literal> clause) {
	}

	@Override
	public void addGuardClauses(Set<Clause<? extends Literal>> clauses) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getActionName() {
		if (this.action != null) {
			return this.action.getName();
		}
		return "";
	}

}
