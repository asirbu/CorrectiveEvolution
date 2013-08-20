package eu.fbk.soa.evolution.engine.impl;

import eu.fbk.soa.process.StateFormula;


public class Condition {

	private StateFormula formula;

	private int index;

	Condition(){}
	
	public Condition(StateFormula formula, int index) {
		this.formula = formula;
		this.index = index;
	}

	public StateFormula getFormula() {
		return formula;
	}

	public int getIndex() {
		return index;
	}

	public boolean isEmpty() {
		return (this.formula == null || this.formula.isEmpty());
	}
	
	public boolean equals(Object obj) {
		if (obj instanceof Condition) {
			Condition cond = (Condition) obj;
			if (this.index == cond.index && 
					this.formula.equals(cond.getFormula())) {
				return true;
			}
		}
		return false;
	}
	
	public int hashCode() {
		return index + formula.hashCode();
	}

	public Condition getNegation() {
		StateFormula negatedFormula = this.formula.getNegation();
		return new Condition(negatedFormula, index);
	}
	
	public String toString() {
		return "Condition " + index + ": " + this.formula;
	}
}
