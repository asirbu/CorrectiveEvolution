package eu.fbk.soa.process.domain;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;



@XmlRootElement(name = "object", namespace = "http://soa.fbk.eu/Object")
@XmlAccessorType(XmlAccessType.FIELD)
public class DomainObject {

	@XmlAttribute(name = "name", required = true)
	private String name;

	@XmlElementWrapper(name = "states", namespace = "http://soa.fbk.eu/Object")
	@XmlElements(@XmlElement(name="state", namespace = "http://soa.fbk.eu/Object"))  
	private Set<ObjectState> states;

	@XmlElementWrapper(name = "initialStates", namespace = "http://soa.fbk.eu/Object")
	@XmlElements(@XmlElement(name = "state", namespace = "http://soa.fbk.eu/Object"))  
	private Set<ObjectState> initialStates;

	@XmlElementWrapper(namespace = "http://soa.fbk.eu/Object")
	@XmlElements(@XmlElement(name = "event", namespace = "http://soa.fbk.eu/Object"))  
	private Set<ObjectEvent> events;

	@XmlElementWrapper(namespace = "http://soa.fbk.eu/Object")
	@XmlElements(@XmlElement(name="transition", namespace = "http://soa.fbk.eu/Object"))  
	private Set<ObjectTransition> transitions;
	
	public DomainObject(String name) {
		this();
		this.name = name;		
	}

	public DomainObject(String name, Collection<ObjectState> totalSt,
			Collection<ObjectState> initialSt, Collection<ObjectEvent> events,
			Collection<ObjectTransition> transitions) {

		for (ObjectState initState : initialSt) {
			if (!totalSt.contains(initState)) {
				throw new IllegalArgumentException("Could not create object "
						+ "diagram: initial state " + initState
						+ " not contained " + "in the total set of states");
			}
		}

		this.name = name;
		this.states = new HashSet<ObjectState>(totalSt);
		this.initialStates = new HashSet<ObjectState>(initialSt);
		this.events = new HashSet<ObjectEvent>(events);
		this.transitions = new HashSet<ObjectTransition>(transitions);
	}
	
	public DomainObject() {
		this.states = new HashSet<ObjectState>();
		this.initialStates = new HashSet<ObjectState>();
		this.events = new HashSet<ObjectEvent>();
		this.transitions = new HashSet<ObjectTransition>();
	}

	public String getName() {
		return name;
	}

	public Set<ObjectState> getStates() {
		return this.states;
	}

	public Set<ObjectEvent> getEvents() {
		return events;
	}

	public ObjectState getInitialState() {
		Iterator<ObjectState> iterator = initialStates.iterator();
		if (iterator.hasNext()) {
			return initialStates.iterator().next();
		}
		return null; 
	}
	
	public Set<ObjectState> getInitialStates() {
		return initialStates;
	}

	public Set<ObjectTransition> getTransitions() {
		return this.transitions;
	}

	public void setInitialState(ObjectState newInitialState) {
		this.initialStates = new HashSet<ObjectState>();
		initialStates.add(newInitialState);
	}
	
	public boolean equals(Object object) {
		if (!(object instanceof DomainObject)) {
			return false;
		}
		DomainObject domObj = (DomainObject) object;
		boolean equal = 		
				(this.name.equals(domObj.getName()) &&
				this.initialStates.equals(domObj.getInitialStates()) &&
				this.states.equals(domObj.getStates()) &&
				this.events.equals(domObj.getEvents()) &&
				this.transitions.equals(domObj.getTransitions()));	
//		System.out.println("Comparing " + this.name + " with " + domObj.getName() + ": " + 
//				equal);
		return equal;
	}
	
	public int hashCode() {
		return this.name.hashCode() + this.states.hashCode() + 
			this.events.hashCode() + this.transitions.hashCode();
	}

	public Set<ObjectTransition> getTransitionsToState(ObjectState state) {
		Set<ObjectTransition> trans2state = new HashSet<ObjectTransition>();
		for (ObjectTransition trans : this.transitions) {
			if (trans.getEndState().equals(state)) {
				trans2state.add(trans);
			}
		}
		return trans2state;
	}

	public void updateEventReferences() {
		for (ObjectTransition trans : transitions) {
			trans.updateEventReference(events);
		}
	}
	
	public ObjectState getStateByName(String stateName) {
		for (ObjectState state : this.states) {
			if (state.getName().equals(stateName)) {
				return state;
			}
		}
		return null;
	}

}
