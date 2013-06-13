package eu.fbk.soa.xml;

import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import eu.fbk.soa.process.Activity;

@XmlRootElement(name="activitySet", namespace = "http://soa.fbk.eu/Process")
@XmlAccessorType(XmlAccessType.NONE)
public class XMLActivitySet {

	@XmlElements({
		@XmlElement(name = "activity", namespace = "http://soa.fbk.eu/Process")
	})
	private Set<Activity> activities;
	
	public XMLActivitySet() {
		this.activities = new HashSet<Activity>();
	}

	public Set<Activity> getActivities() {
		return activities;
	}

	public void setActivities(Set<Activity> activities) {
		this.activities = activities;
	}
	
}
