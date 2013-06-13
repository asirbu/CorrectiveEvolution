package eu.fbk.soa.process.domain;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

@XmlType(name = "event", namespace = "http://soa.fbk.eu/Object")
@XmlAccessorType(XmlAccessType.NONE)
public class ObjectEvent {

	@XmlValue
	private String eventName;
	
	@XmlAttribute(name = "isControllable", required = true)
	private boolean isControllable;
	
	public ObjectEvent() {
		this.isControllable = true;
	}

	public ObjectEvent(String name) {
		this(name, true);
	}

	public ObjectEvent(String name, boolean isControllable) {
		this.eventName = name;
		this.isControllable = isControllable;
	}
	
	public String getName() {
		return eventName;
	}

	public String toString() {
		return eventName;
	}
	
	public boolean equals(Object obj) {
		if (obj instanceof ObjectEvent) {
			ObjectEvent objEvent = (ObjectEvent) obj;
			
			if (eventName.equals(objEvent.getName()) 
					&& this.isControllable == objEvent.isControllable()) {
				return true;
			}
		}
		
		return false;
	}

	public int hashCode() {
		return eventName.hashCode();
	}

	public void setControllable(boolean isControllable) {
		this.isControllable = isControllable;
	}

	public boolean isControllable() {
		return isControllable;
	}
}
