package eu.fbk.soa.xml;

import java.util.Set;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import eu.fbk.soa.process.StateFormula;
import eu.fbk.soa.process.domain.DomainObject;
import eu.fbk.soa.process.node.ProcessNode;

@XmlRootElement(name="edge", namespace = "http://soa.fbk.eu/Process")
public class XMLProcessEdge {

	@XmlAttribute(name = "source", required = true)
	private String sourceID;
	
	@XmlAttribute(name = "target", required = true)
	private String targetID;
	
	@XmlElement(name = "condition", type = eu.fbk.soa.process.StateFormula.class, namespace = "http://soa.fbk.eu/Process")
	private StateFormula condition = new StateFormula();

	private ProcessNode source;
	
	private ProcessNode target;
	
	public XMLProcessEdge() {}
	
	public void updateReferences(Set<ProcessNode> nodes, Set<DomainObject> objects)
		throws XMLParsingException {
		
		source = getNodeByID(sourceID, nodes);
		target = getNodeByID(targetID, nodes);
		condition.updateObjectReferences(objects);
	}
	
	private ProcessNode getNodeByID(String nodeID, Set<ProcessNode> nodes) 
		throws XMLParsingException {
		
		for (ProcessNode node : nodes) {
			if (node.getNodeID().equals(nodeID)) {
				return node;
			}
		}
		throw new XMLParsingException(
				"Parsing edges: could not find declaration of node " + nodeID);
	}

	public ProcessNode getSource() {
		return source;
	}
	
	public ProcessNode getTarget() {
		return target;
	}

	public StateFormula getCondition() {
		return condition;
	}
	
}
