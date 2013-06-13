package eu.fbk.soa.process.node;

import java.util.HashSet;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import eu.fbk.soa.process.domain.DomainObject;


@XmlAccessorType(XmlAccessType.NONE)
public abstract class ProcessNode {

	private static int nodeIndex = 0; 
	
	int nodeNr;
	
	@XmlAttribute
	private String nodeID = "";
	
	public ProcessNode() {
		this.nodeNr = nodeIndex;
		nodeIndex++;
	}
	
	public Set<DomainObject> getRelatedDomainObjects() {
		return new HashSet<DomainObject>();
	}

	public String getNodeID() {
		if (nodeID == "") {
			nodeID = "node" + this.nodeNr;
		}
		return nodeID;
	}
}
