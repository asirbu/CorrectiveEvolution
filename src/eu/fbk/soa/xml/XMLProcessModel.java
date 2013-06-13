package eu.fbk.soa.xml;

import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import eu.fbk.soa.process.Activity;
import eu.fbk.soa.process.domain.DomainObject;
import eu.fbk.soa.process.node.ActivityNode;
import eu.fbk.soa.process.node.ProcessNode;

@XmlRootElement(name = "process", namespace = "http://soa.fbk.eu/Process")
@XmlAccessorType(XmlAccessType.NONE)
public class XMLProcessModel {

	@XmlAttribute(name = "name", required = true)
	private String name;

	@XmlElementWrapper(name = "activities", namespace = "http://soa.fbk.eu/Process")
	@XmlElements({
		@XmlElement(name = "activity", namespace = "http://soa.fbk.eu/Process")
	})
	private Set<Activity> activities;


	@XmlElementWrapper(name = "nodes", namespace = "http://soa.fbk.eu/Process")
	@XmlElements({
		@XmlElement(name = "activityNode", type = eu.fbk.soa.process.node.ActivityNode.class, namespace = "http://soa.fbk.eu/Process"),
		@XmlElement(name = "startNode", type = eu.fbk.soa.process.node.StartNode.class, namespace = "http://soa.fbk.eu/Process"),
		@XmlElement(name = "endNode", type = eu.fbk.soa.process.node.EndNode.class, namespace = "http://soa.fbk.eu/Process"),
		@XmlElement(name = "andSplit", type = eu.fbk.soa.process.node.AndSplit.class, namespace = "http://soa.fbk.eu/Process"),
		@XmlElement(name = "andJoin", type = eu.fbk.soa.process.node.AndJoin.class, namespace = "http://soa.fbk.eu/Process"),
		@XmlElement(name = "xorSplit", type = eu.fbk.soa.process.node.XorSplit.class, namespace = "http://soa.fbk.eu/Process"),
		@XmlElement(name = "xorJoin", type = eu.fbk.soa.process.node.XorJoin.class, namespace = "http://soa.fbk.eu/Process")		
	})	
	private Set<ProcessNode> nodes;


	@XmlElementWrapper(name = "edges", namespace = "http://soa.fbk.eu/Process")
	@XmlElements({
		@XmlElement(name = "edge", type = eu.fbk.soa.xml.XMLProcessEdge.class, namespace = "http://soa.fbk.eu/Process"),
	})
	private Set<XMLProcessEdge> edges;


	public XMLProcessModel() {
		this.activities = new HashSet<Activity>();
		this.nodes = new HashSet<ProcessNode>();
		this.edges = new HashSet<XMLProcessEdge>();
	}

	
	public XMLProcessModel(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public Set<Activity> getActivities() {
		return activities;
	}

	public Set<ProcessNode> getNodes() {
		return this.nodes;
	}


	public Set<XMLProcessEdge> getEdges() {
		return edges;
	}


	public void updateReferences(Set<DomainObject> objects) 
	throws XMLParsingException {

		for (Activity act : this.activities) {
			act.updateObjectReferences(objects);
			if (act.hasUnresolvedReferences()) {
				throw new XMLParsingException("Could not resolve object references for activity" + act.getName()); 
			}
		}

//		Set<ProcessNode> nodesCopy = new HashSet<ProcessNode>(nodes);
		for (ProcessNode node : this.nodes) {
			if (node instanceof ActivityNode) {
				ActivityNode actNode = (ActivityNode) node;
				String actName = actNode.getActivityName();
				for (Activity act : activities) {
					if (act.getName().equals(actName)) {
						actNode.setActivity(act);
						break;
					}
				}
			}
		}
		
		for (XMLProcessEdge edge : edges) {
			edge.updateReferences(nodes, objects);	
		}
	}


	public void setActivities(Set<Activity> acts) {
		this.activities = acts;
	}	
	
	
	
}
