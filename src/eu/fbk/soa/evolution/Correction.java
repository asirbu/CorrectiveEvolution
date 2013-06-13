package eu.fbk.soa.evolution;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;

import eu.fbk.soa.process.Adaptation;
import eu.fbk.soa.process.ProcessModel;
import eu.fbk.soa.process.StateFormula;
import eu.fbk.soa.process.Trace;
import eu.fbk.soa.process.domain.DomainObject;

public class Correction {

	private static Logger logger = Logger.getLogger(Correction.class);
	
	private static int nrCorrections = 0;
	
	public enum Type {
		STRICT, RELAXED, WITH_CONDITIONS};
		
	private Type type;
	
	private Trace trace;
	
	private StateFormula condition;
	
	private Adaptation adaptation;
	
	private String name;
	
	public Type getType() {
		return type;
	}

	public Trace getTrace() {
		return trace;
	}

	public StateFormula getCondition() {
		return condition;
	}
	
	public Correction(Type type, Trace trace, StateFormula cond, Adaptation ad) {	
		nrCorrections++;
		this.name = "C" + nrCorrections;
		this.type = type;
		this.trace = trace;
		this.condition = cond;
		this.adaptation = ad;
	}

	public boolean isStrict() {
		return (this.type == Type.STRICT);
	}

	public boolean isRelaxed() {
		return (this.type == Type.RELAXED);
	}
	
	public boolean isRelaxedWithConditions() {
		return (this.type == Type.WITH_CONDITIONS);
	}

	
	/**
	 * A correction is applicable to a process model if:
	 * - the trace is a partial trace on the model
	 * - the adaptation is applicable to the model on the trace 
	 * (or on some trace, if the input trace is the empty set)
	 */
	public boolean isApplicable(ProcessModel model) {
		Set<DomainObject> condObjects = condition.getRelatedDomainObjects();
		
		Set<DomainObject> objectsInModels = new HashSet<DomainObject>();
		objectsInModels.addAll(model.getRelatedDomainObjects());
		objectsInModels.addAll(
				adaptation.getRelatedDomainObjects());
		
		boolean validTrace = trace.isValidOnProcessModel(model);
//		boolean validObjects = objectsInModels.containsAll(condObjects);
		boolean applicableAdaptation = adaptation.isApplicable(model, trace);
		
		if (!validTrace) {
			logger.error("Invalid trace");
		}
		
		if (!applicableAdaptation) {
			logger.error("Adaptation is not applicable");
		}
		
		return (
//				objectsInModels.containsAll(condObjects) &&
//				trace.isValidOnProcessModel(model) &&
				adaptation.isApplicable(model, trace));	
		
	}

	public Set<DomainObject> getRelatedDomainObjects() {
		Set<DomainObject> objects = new HashSet<DomainObject>();
		
		objects.addAll(adaptation.getRelatedDomainObjects());
		objects.addAll(condition.getRelatedDomainObjects());
		
		return objects;
	}
	
	public Adaptation getAdaptation() {
		return adaptation;
	}
	
	public String toString() {
		String str = "correction " + this.name;
		
		if (!trace.isEmpty()) {
			str += "\n\t - executed trace: " + this.trace;
		}
		
		str += "\n\t - condition: " + this.condition;
		
		str += "\n\t - " + this.adaptation;
		
		return str;
	}

	public boolean hasSameSetting(Correction correction) {
//		logger.trace("Comparing correction\n\t " + this.toString() +
//				"\n with correction \n\t" + correction.toString());
		
		if (this.condition.equals(correction.getCondition()) &&
				this.isAtSamePoint(correction))  {
			return true;
		}
		
		return false;
	}
	
	public boolean isAtSamePoint(Correction correction) {
		if (this.trace.equals(correction.getTrace()) &&
				this.adaptation.getFromNode().equals(correction.getAdaptation().getFromNode()))  {
			return true;
		}
		
		return false;
	}
	
	public boolean isSameExceptForFromNode(Correction corr) {
		return (this.type.equals(corr.getType()) 
				&& this.trace.equals(corr.getTrace()) 
				&& this.condition.equals(corr.getCondition())
				&& this.adaptation.getAdaptationModel().shallowEquals(corr.getAdaptation().getAdaptationModel()) 
				&& this.adaptation.getToNode().equals(corr.getAdaptation().getToNode()));
	}
	
	public String getName() {
		return this.name;
	}
	
	public boolean equals(Object obj) {
		if (!(obj instanceof Correction)) {
			return false;
		}
		Correction corr = (Correction) obj;
		return this.type.equals(corr.getType()) 
				&& this.trace.equals(corr.getTrace()) 
				&& this.adaptation.equals(corr.getAdaptation())
				&& this.condition.equals(corr.getCondition());
	}

}
