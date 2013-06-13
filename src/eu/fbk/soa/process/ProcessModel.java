package eu.fbk.soa.process;

import java.util.Set;

import eu.fbk.soa.process.domain.DomainObject;
import eu.fbk.soa.process.node.ActivityNode;
import eu.fbk.soa.process.node.AndSplit;
import eu.fbk.soa.process.node.ProcessNode;
import eu.fbk.soa.process.node.StartNode;

public interface ProcessModel {

	/* (non-Javadoc)
	 * @see eu.fbk.soa.process.MyProcessModel#addEdge(eu.fbk.soa.process.node.ProcessNode, eu.fbk.soa.process.node.ProcessNode, eu.fbk.soa.process.StateFormula)
	 */
	public ProcessEdge addEdge(ProcessNode source, ProcessNode target,
			StateFormula condition);

	/* (non-Javadoc)
	 * @see eu.fbk.soa.process.MyProcessModel#addNode(eu.fbk.soa.process.node.ProcessNode)
	 */
	public boolean addNode(ProcessNode node);

	public boolean containsEdge(ProcessNode node1, ProcessNode node2);

	public boolean containsProcessNode(ProcessNode node);

	public Set<ProcessEdge> edgeSet();

	public AndSplit getContainingAndBlock(ActivityNode actNode);

	/* (non-Javadoc)
	 * @see eu.fbk.soa.process.MyProcessModel#getDistinctTraces()
	 */
	public Set<Trace> getDistinctTraces();

	public ProcessEdge getEdge(ProcessNode fromNode, ProcessNode toNode);

	public ProcessNode getEdgeSource(ProcessEdge edge);

	public ProcessNode getEdgeTarget(ProcessEdge edge);

	public ActivityNode getFirstActivityNode();

	public ActivityNode getLastActivityNode();

	public String getName();

	public Set<ProcessNode> getProcessNodes();

	/* (non-Javadoc)
	 * @see eu.fbk.soa.process.MyProcessModel#getProcessNodes(eu.fbk.soa.process.Activity)
	 */
	public Set<ProcessNode> getProcessNodes(Activity activity);

	/* (non-Javadoc)
	 * @see eu.fbk.soa.process.MyProcessModel#getRelatedDomainObjects()
	 */
	public Set<DomainObject> getRelatedDomainObjects();

	/* (non-Javadoc)
	 * @see eu.fbk.soa.process.MyProcessModel#getStartNode()
	 */
	public StartNode getStartNode();

	/* (non-Javadoc)
	 * @see eu.fbk.soa.process.MyProcessModel#isRelatedTo(eu.fbk.soa.process.domain.DomainObject)
	 */
	public boolean isRelatedTo(DomainObject obj);

	public void setStartNode(ActivityNode node);

	public boolean shallowEquals(ProcessModel pm);

	public Set<ProcessEdge> outgoingEdgesOf(ProcessNode node);

	public Set<ProcessEdge> incomingEdgesOf(ProcessNode node);

	public int inDegreeOf(ProcessNode node);
	
	public int outDegreeOf(ProcessNode node);

	public Set<ProcessEdge> edgesOf(ProcessNode node);

	public ProcessEdge addEdge(ProcessNode node1, ProcessNode node2);

}