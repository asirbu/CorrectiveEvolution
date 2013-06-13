package eu.fbk.soa.process.domain;

import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;


@XmlAccessorType(XmlAccessType.NONE)
public class EventLiteral extends ObjectLiteral {
	
	@XmlAttribute(name="event", required = true)
	private String eventName;
	
	private ObjectEvent event;

	public EventLiteral(DomainObject domainObj, ObjectEvent event) {
		this.init(domainObj.getName(), event);
		this.object = domainObj;	
	}

	public EventLiteral() {}
	
	
	public EventLiteral(String objName, ObjectEvent event) {
		this.init(objName, event);
	}
	
	private void init(String objName, ObjectEvent event) {
		this.objectName = objName;
		this.event = event;
		this.eventName = event.getName();
	}
	
	public ObjectEvent getObjectEvent() {
		return event;
	}

	public String getEventName() {
		return eventName;
	}
	
	public EventLiteral getNegation() {
		EventLiteral negation = new EventLiteral(object, event);
//		if (this.object != null) {
//			negation.setDomainObject(this.object);
//		}
		negation.negate();
		return negation;
	}
	
	public boolean equals(Object obj) {
		if (obj instanceof EventLiteral) {
			EventLiteral eLit = (EventLiteral) obj;
			
			if	(this.isNegated != eLit.isNegated() ||
					!this.eventName.equals(eLit.getEventName())) {
				return false;
			}
			
			if (!objectName.equals(eLit.getDomainObjectName())) {
					return false;
			}
			return true;
		}
		return false;
	}

	public ObjectEvent getEvent() {
		return this.event;
	}

	@Override
	public String getProposition() {
		return eventName + "(" + objectName + ")";
	}

	@Override	
	public void updateObjectReferences(Set<DomainObject> objects) {
		for (DomainObject obj : objects) {
			if (obj.getName().equals(this.objectName)) {
				this.object = obj;
				for (ObjectEvent ev : obj.getEvents()) {
					if (this.eventName.equals(ev.getName())) {
						this.event = ev;
						break;
					}
				}
				break;
			}
		}
	}
 
	@Override
	public boolean hasUnresolvedReferences() {
		return (this.object == null || this.event == null);
	}

	public String toString() {
		String str = "";
		if (this.isNegated) {
			str = "-";
		}
		return str + eventName + "(" + objectName + ")";
	}
	
	public int hashCode() {
		return Boolean.toString(isNegated).hashCode() + 
			eventName.hashCode() + objectName.hashCode();
	}

}
