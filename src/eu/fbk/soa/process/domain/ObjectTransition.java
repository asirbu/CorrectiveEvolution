package eu.fbk.soa.process.domain;

import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "transition", namespace = "http://soa.fbk.eu/Object")
public class ObjectTransition {

	@XmlElement(required = true, namespace = "http://soa.fbk.eu/Object")
	private ObjectState startState;

	@XmlElement(required = true, namespace = "http://soa.fbk.eu/Object")
	private ObjectEvent event;

	@XmlElement(required = true, namespace = "http://soa.fbk.eu/Object")
	private ObjectState endState;

	public ObjectTransition(){};
	
	public ObjectTransition(ObjectState start, ObjectEvent event,
			ObjectState end) {

		this.startState = start;
		this.event = event;
		this.endState = end;
	}

	public ObjectState getStartState() {
		return startState;
	}

	public ObjectEvent getObjectEvent() {
		return event;
	}

	public ObjectState getEndState() {
		return endState;
	}

	public boolean equals(Object object) {
		if (!(object instanceof ObjectTransition)) {
			return false;
		}
		ObjectTransition trans = (ObjectTransition) object;
		return (this.startState.equals(trans.getStartState()) &&
				this.event.equals(trans.getObjectEvent()) && 
				this.endState.equals(trans.getEndState()));
	}
	
	public int hashCode() {
		return startState.hashCode() + event.hashCode() + endState.hashCode();
	}
	
	public String toString() {
		return "Transition from " + startState + " to " + endState + " on " + event; 
	}

	public void updateEventReference(Set<ObjectEvent> events) {
		for (ObjectEvent ev : events) {
			if (this.event.getName().equals(ev.getName())) {
				this.event = ev;
				break;
			}
		}	
	}
	
}
