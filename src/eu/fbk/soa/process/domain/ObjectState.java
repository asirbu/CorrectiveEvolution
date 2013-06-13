package eu.fbk.soa.process.domain;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

import eu.fbk.soa.evolution.sts.State;




@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "state", namespace = "http://soa.fbk.eu/Object")
public class ObjectState implements State {

	@XmlValue
	private String stateName;

	public ObjectState(String name) {
		this.stateName = name;
	}

	public ObjectState() {
	}
	
	public String getName() {
		return stateName;
	}

	public String toString() {
		return stateName;
	}
	
	public int hashCode() {
		return stateName.hashCode();
	}
	
	public boolean equals(Object obj) {
		if (obj instanceof ObjectState) {
			ObjectState state = (ObjectState) obj;
			
			return stateName.equals(state.getName());
		}
		return false;
	}

	@Override
	public void setName(String name) {
		this.stateName = name;
	}
}
