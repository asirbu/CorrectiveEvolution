package eu.fbk.soa.process;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import eu.fbk.soa.process.domain.DomainObject;
import eu.fbk.soa.process.domain.EventLiteral;
import eu.fbk.soa.util.StringUtils;


@XmlRootElement(name="effect", namespace = "http://soa.fbk.eu/Process")
@XmlAccessorType(XmlAccessType.NONE)
public class Effect {

	@XmlElements(
		@XmlElement(name = "literal", namespace = "http://soa.fbk.eu/Object"))
	private Set<EventLiteral> events;

	public Effect() {
		events = new HashSet<EventLiteral>();
	}

	public Effect(EventLiteral event) {
		this.events = new HashSet<EventLiteral>();
		events.add(event);
	}
	
	public Effect(EventLiteral... eventArray) {
		this.events = new HashSet<EventLiteral>();
		for (EventLiteral p : eventArray) {
			events.add(p);
		}
	}
	

	public Effect(Collection<EventLiteral> events) {
		this.events = new HashSet<EventLiteral>(events);
	}

	

	public Set<EventLiteral> getEventLiterals() {
		return events;
	}

	public boolean isRelatedTo(DomainObject domainObj) {
		for (EventLiteral evProp : events) {
			if (evProp.isRelatedTo(domainObj)) {
				return true;
			}
		}
		return false;
	}


	public void add(EventLiteral eventProposition) {
		events.add(eventProposition);
	}

	public boolean isEmpty() {
		return this.events.isEmpty();
	}

	public void addAll(Collection<EventLiteral> eventLiterals) {
		events.addAll(eventLiterals);
	}
	
	public void addEffect(Effect otherEffect) {
		events.addAll(otherEffect.getEventLiterals());
	}

	public void updateObjectReferences(Set<DomainObject> objects) {
		for (EventLiteral lit : events) {
			lit.updateObjectReferences(objects);
		}
	}
	
	public Set<DomainObject> getRelatedDomainObjects() {
		Set<DomainObject> objects = new HashSet<DomainObject>();
		
		for (EventLiteral lit : events) {
			objects.add(lit.getDomainObject());
		}
		return objects;
	}

	public boolean contains(EventLiteral evLit) {
		return this.events.contains(evLit);
	}
	
	public boolean equals(Object object) {
		if (object instanceof Effect) {
			Effect eff = (Effect) object;
			for (EventLiteral evLit : this.events) {
				if (!eff.contains(evLit)) {
					return false;
				}
			}
			for (EventLiteral evLit : eff.getEventLiterals()) {
				if (!this.contains(evLit)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	public String toString() {
		return "{" + StringUtils.getCommaSeparatedString(events) + "}";
	}
	
	public boolean hasUnresolvedReferences() {
		for (EventLiteral lit : events) {
			if (lit.hasUnresolvedReferences()) {
				return true;
			}
		}
		return false;
	}
	
}
