package eu.fbk.soa.process.domain;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;


@XmlAccessorType(XmlAccessType.NONE)
public class StateLiteral extends ObjectLiteral {
	
	@XmlAttribute(name="state", required = true)
	private ObjectState state;
	
	public StateLiteral(){}
	
	public StateLiteral(DomainObject domainObj, ObjectState state) {
		init(domainObj.getName(), state, false);
		this.object = domainObj;
	}
	
	public StateLiteral(DomainObject domainObj, ObjectState state, boolean isNegated) {
		init(domainObj.getName(), state, isNegated);
		this.object = domainObj;
	}
		
	private void init(String objName, ObjectState state, boolean isNegated) {
		this.objectName = objName;
		this.state = state;
		this.isNegated = isNegated;
	}
	
	public ObjectState getState() {
		return state;
	}

	public String getStateName() {
		return state.getName();
	}

	public StateLiteral getNegation() {
		StateLiteral negation = new StateLiteral(object, state, !isNegated);
		return negation;
	}
	
	public boolean equals(Object obj) {
		//System.out.println("Testing if " + this.toString() + " equals " + obj.toString());
		if (obj instanceof StateLiteral) {
			StateLiteral sLit = (StateLiteral) obj;
			
			if (this.isNegated != sLit.isNegated()) {
				return false;
			}
			
			if (!state.equals(sLit.getState())) {
				return false;
			}
			
			if (!objectName.equals(sLit.getDomainObjectName())) {
				return false;
			}
			return true;
		}
		return false;
	}

	@Override
	public String getProposition() {
		return state.getName() + "(" + objectName + ")";
	}
	
	public String toString() {
		if (this.isNegated) {
			return "not(" + this.getProposition() + ")";
		} else {
			return this.getProposition();
		}
	}
	
	public int hashCode() {
		return Boolean.toString(isNegated).hashCode() + 
			state.getName().hashCode() + objectName.hashCode();
	}
	
}
