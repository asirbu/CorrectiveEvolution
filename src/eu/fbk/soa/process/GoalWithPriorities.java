package eu.fbk.soa.process;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import eu.fbk.soa.process.domain.DomainObject;


@XmlRootElement(name = "goal", namespace = "http://soa.fbk.eu/Goal")
@XmlAccessorType(XmlAccessType.FIELD)
public class GoalWithPriorities implements Goal {

	
	@XmlElement(name = "premise", namespace = "http://soa.fbk.eu/Goal")
	private StateFormula premise;

	
	@XmlElementWrapper(name = "result", namespace = "http://soa.fbk.eu/Goal")
	@XmlElements({
		@XmlElement(name = "formula", namespace = "http://soa.fbk.eu/Goal")
	})
	private List<StateFormula> result;

	public GoalWithPriorities() {
		premise = new StateFormula();
		result = new ArrayList<StateFormula>();
	}

	public GoalWithPriorities(StateFormula premise, List<StateFormula> result) {
		this.premise = premise;
		this.result = result;
	}

	public GoalWithPriorities(StateFormula premiseFormula, StateFormula resultFormula) {
		this.premise = premiseFormula;
		this.result = new ArrayList<StateFormula>();
		this.result.add(resultFormula);
	}

	public StateFormula getPremise() {
		return premise;
	}

	public List<StateFormula> getResult() {
		return result;
	}


	public void updateObjectReferences(Set<DomainObject> objects) {
		premise.updateObjectReferences(objects);
		for (StateFormula formula : result) {
			formula.updateObjectReferences(objects);
		}
		
	}

	public void setResult(List<StateFormula> result) {
		this.result = result;
	}

}
