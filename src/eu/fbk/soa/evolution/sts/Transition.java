package eu.fbk.soa.evolution.sts;

import java.util.Set;

import eu.fbk.soa.process.StateFormula;



public interface Transition {

	public State getSource();

	public Action getAction();
	
	public String getActionName();
	
	public State getTarget();

	public Transition replaceState(State state, State replacement);

	public String toDot();

	public StateFormula getCondition();

	public void setCondition(StateFormula cond);
	
	public Set<Clause<? extends Literal>> getGuardClauses();
	
	public void addGuardClause(Clause<? extends Literal> clause);
	
	public void addGuardClauses(Set<Clause<? extends Literal>> clauses);

}