package eu.fbk.soa.process;

import java.util.Set;

import eu.fbk.soa.process.domain.DomainObject;


public class EaGLeGoal implements Goal {

	public enum Operator {
		Default, DoReach, TryReach, DoMaint, TryMaint};
	
	private Operator operator;	
		
	private StateFormula formula;
	
	public EaGLeGoal(StateFormula formula) {
		this.operator = Operator.Default;
		this.formula = formula;
	}
	
	public EaGLeGoal(Operator operator, StateFormula formula) {
		this.operator = operator;
		this.formula = formula;
	}

	public EaGLeGoal() {
		this(new StateFormula());
	}

	public Operator getOperator() {
		return operator;
	}

	public StateFormula getFormula() {
		return formula;
	}

	public boolean isEmpty() {
		return this.formula.isEmpty();
	}

	@Override
	public void updateObjectReferences(Set<DomainObject> objects) {
		formula.updateObjectReferences(objects);
	}

	
}
