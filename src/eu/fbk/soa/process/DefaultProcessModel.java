package eu.fbk.soa.process;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.log4j.Logger;
import org.jgrapht.graph.DefaultDirectedGraph;

import eu.fbk.soa.process.domain.DomainObject;
import eu.fbk.soa.process.node.ActivityNode;
import eu.fbk.soa.process.node.AndJoin;
import eu.fbk.soa.process.node.AndSplit;
import eu.fbk.soa.process.node.EndNode;
import eu.fbk.soa.process.node.ProcessNode;
import eu.fbk.soa.process.node.StartNode;
import eu.fbk.soa.process.node.XorSplit;
import eu.fbk.soa.util.ArrayUtils;

@XmlRootElement(name="process", namespace = "http://soa.fbk.eu/Process")
@XmlAccessorType(XmlAccessType.NONE)
public class DefaultProcessModel 
	extends DefaultDirectedGraph<ProcessNode, ProcessEdge> implements ProcessModel {
	
	private static Logger logger = Logger.getLogger(DefaultProcessModel.class);
	
	private static final long serialVersionUID = 2178756401214070327L;
	
	private static int maxTraceLength = 40;
	
	@XmlAttribute(name ="name", required = true)
	private String name;
	
	private Set<DomainObject> objects = new HashSet<DomainObject>();
	
	private StartNode startNode;
	
	public DefaultProcessModel(String modelName) {
		super(ProcessEdge.class);
		this.name = modelName;
		
	}
	
	public DefaultProcessModel(String modelName, Collection<ProcessNode> nodes) {
		this(modelName);
		for (ProcessNode node : nodes) {
			addNode(node);
		}	
	}
	
	@Override
	public boolean addNode(ProcessNode node) {
		boolean exists = this.addVertex(node);
		objects.addAll(node.getRelatedDomainObjects());
		if (node instanceof StartNode) {
			this.startNode = (StartNode) node;
		}
		return exists;
	}
	
	@Override
	public ProcessEdge addEdge(ProcessNode source, ProcessNode target, StateFormula condition) {
		ProcessEdge edge = this.addEdge(source, target);
		if (condition.isEmpty() || source instanceof XorSplit) {
			edge.setCondition(condition);
			objects.addAll(condition.getRelatedDomainObjects());
		} else {
			throw new IllegalArgumentException("Can only add conditions to edges " +
			"starting from XorSplit nodes");
		}
		return edge;
	}
	
	@Override
	public Set<ProcessNode> getProcessNodes() {
		return this.vertexSet();
	}
	
	@Override
	public Set<ProcessNode> getProcessNodes(Activity activity) {
		Set<ProcessNode> nodes = new HashSet<ProcessNode>();
		
		for (ProcessNode node : this.vertexSet()) {
			if (node instanceof ActivityNode) {
				ActivityNode actNode = (ActivityNode) node;
				if (actNode.pointsTo(activity)) {
					nodes.add(actNode);
				}
			}
		}
		return nodes;
	}
	
	@Override
	public Set<DomainObject> getRelatedDomainObjects() {
		return this.objects;
	}

	@Override
	public boolean isRelatedTo(DomainObject obj) {
		return (objects.contains(obj));
	}
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public StartNode getStartNode() {
		return startNode;
	}

	@Override
	public Set<Trace> getDistinctTraces() {
		Set<Trace> traces = getTracesFromNode(startNode, maxTraceLength);
		return traces;
	}

	// TODO What about loops?
	private Set<Trace> getTracesFromNode(ProcessNode node, int steps) {
		Set<Trace> traces = new HashSet<Trace>();
		
		if (node instanceof EndNode || steps == 0) {
			Trace newTrace = new Trace();
			traces.add(newTrace);
			return traces;
		}
		
		if (node instanceof ActivityNode) {
			return getTracesFromActivityNode((ActivityNode) node, steps);
		}
		if (node instanceof AndSplit) {
			AndJoin andJoin = this.getMatchingAndJoin((AndSplit) node, steps-1);
			Set<Trace> partTraces = this.getTracesInAndBlock((AndSplit) node, andJoin, steps);
			Set<Trace> nextTraces = getTracesFromNode(andJoin, steps-1);
			for (Trace t1 : partTraces) {
				for (Trace t2 : nextTraces) {
					Trace t = new Trace(t1.getActivities());
					t.addAllActivities(t2.getActivities());
					traces.add(t);
				}
			}
		}
		for (ProcessEdge edge : this.outgoingEdgesOf(node)) {
			Set<Trace> outTraces = getTracesFromNode(this.getEdgeTarget(edge), steps-1);
			traces.addAll(outTraces);
		}
		return traces;
	}
	
	private Set<Trace> getTracesFromActivityNode(ActivityNode actNode, int steps) {
		Set<Trace> traces = new HashSet<Trace>();
		
		Activity myAct = actNode.getActivity();
		
		if (this.outDegreeOf(actNode) == 0) {
			Trace newTrace = new Trace();
			newTrace.addActivity(myAct);
			traces.add(newTrace);
			return traces;
		}
		
		for (ProcessEdge edge : this.outgoingEdgesOf(actNode)) {
			Set<Trace> outTraces = getTracesFromNode(this.getEdgeTarget(edge), steps-1);
			for (Trace outTrace : outTraces) {
				Trace newTrace = new Trace();
				newTrace.addActivity(myAct);
				newTrace.addAllActivities(outTrace.getActivities());
				traces.add(newTrace);
			} 
		}
		return traces;
	}
	
	
	private Set<Trace> getTracesInAndBlock(AndSplit split, AndJoin andJoin, int steps) {
		Set<Trace> traces = new HashSet<Trace>();
		
//		if (this.outDegreeOf(split) != 2) {
//			throw new UnsupportedOperationException("And-blocks with more then two branches " +
//				"are currently not supported.");
//		}
		
		Map<Integer, Trace> branches = new HashMap<Integer, Trace>();
		int index = 0;
		for (ProcessEdge edge : this.outgoingEdgesOf(split)) {
			index++;
			Set<Trace> outTraces = getTracesFromNodeToNode(this.getEdgeTarget(edge), andJoin, steps-1);
			if (outTraces.size() > 1) {
				throw new UnsupportedOperationException("XorSplits in And-blocks are currently not supported");
			}
			branches.put(index, outTraces.iterator().next());
		}
		
		List<Activity> activities = new ArrayList<Activity>();
		for (Integer key : branches.keySet()) {
			Trace t = branches.get(key);
			if (t.size() > 1) {
				throw new UnsupportedOperationException(
						"And-blocks with more then one activity in one branch are currently not supported.");
			}
			activities.addAll(t.getActivities());
		}
		traces.addAll(this.getAllInterleavings(activities));
		return traces;
	}
	
	private Set<Trace> getAllInterleavings(List<Activity> actList) {
		Set<Trace> interleavings = new HashSet<Trace>();
		
		ArrayUtils<Activity> utils = new ArrayUtils<Activity>();
		for (List<Activity> perm : utils.computePermutations(actList)) {
			Trace t = new Trace(perm);
			interleavings.add(t);
		}
		return interleavings;
	}
	
	
	private Set<Trace> getTracesFromNodeToNode(ProcessNode node1, ProcessNode node2, int steps) {
		Set<Trace> traces = new HashSet<Trace>();
		
		if (node1 instanceof EndNode || steps == 0) {
			Trace newTrace = new Trace();
			traces.add(newTrace);
			return traces;
		}
		
		if (node1 instanceof ActivityNode) {
			return getTracesFromActivityNodeToNode((ActivityNode) node1, node2, steps);
		}
		
		for (ProcessEdge edge : this.outgoingEdgesOf(node1)) {
			Set<Trace> outTraces = getTracesFromNodeToNode(this.getEdgeTarget(edge), node2, steps-1);
			traces.addAll(outTraces);
		}
		return traces;
		
	}
	
	private Set<Trace> getTracesFromActivityNodeToNode(ActivityNode actNode, ProcessNode node, int steps) {
		Set<Trace> traces = new HashSet<Trace>();
		
		Activity myAct = actNode.getActivity();
		
		if (this.outDegreeOf(actNode) == 0) {
			Trace newTrace = new Trace();
			newTrace.addActivity(myAct);
			traces.add(newTrace);
			return traces;
		}
		
		for (ProcessEdge edge : this.outgoingEdgesOf(actNode)) {
			ProcessNode target = this.getEdgeTarget(edge);
			if (target.equals(node)) {
				Trace newTrace = new Trace();
				newTrace.addActivity(myAct);
				traces.add(newTrace);
			} else {
				Set<Trace> outTraces = getTracesFromNodeToNode(target, node, steps-1);
				for (Trace outTrace : outTraces) {
					Trace newTrace = new Trace();
					newTrace.addActivity(myAct);
					newTrace.addAllActivities(outTrace.getActivities());
					traces.add(newTrace);
				} 
			}
		}
		return traces;
	}
	
	
	// TODO there can be loops in the AndBlock, therefore I need to take into account also the steps
	private AndJoin getMatchingAndJoin(AndSplit split, int steps) {
		
		if (!this.containsVertex(split)) {
			return null;
		}
		
		AndJoin matchingAndJoin = null;
		List<ProcessNode> queue = new ArrayList<ProcessNode>();
		
		for (ProcessEdge edge : this.outgoingEdgesOf(split)) {
			ProcessNode target = this.getEdgeTarget(edge);
			queue.add(target);
		}
		
		while (!queue.isEmpty()) {
			ProcessNode node = queue.remove(0);
			if (node instanceof AndJoin) {
				AndJoin andJoin = (AndJoin) node;
				if (matchingAndJoin != null && !matchingAndJoin.equals(andJoin)) {
					throw new UnsupportedOperationException("AndSplit nodes can currently " +
							"have only one corresponding AndJoin node.");
				}
				matchingAndJoin = andJoin;
			} else {
				for (ProcessEdge edge : this.outgoingEdgesOf(node)) {
					queue.add(this.getEdgeTarget(edge));
				}
			}	
		}
		
		return matchingAndJoin;
	}
	
	

	@Override
	public void setStartNode(ActivityNode node) {
		
		StartNode newStartNode = new StartNode(node.getActivity());
		this.addVertex(newStartNode);
		this.startNode = newStartNode;
		
		Set<ProcessEdge> outEdges = this.outgoingEdgesOf(node);
		for (ProcessEdge edge : outEdges) {
			this.addEdge(newStartNode, this.getEdgeTarget(edge), edge.getCondition());
		}
		
		this.removeVertex(node);
	}

	@Override
	public ActivityNode getLastActivityNode() {
		for (ProcessNode pn : this.vertexSet()) {
			if (this.outDegreeOf(pn) == 0) {
				ActivityNode actNode = getLastActivityNode(pn);
				if (actNode != null) {
					return actNode;
				}
			}
		}
		return null;
	}

	private ActivityNode getLastActivityNode(ProcessNode pn) {
		if (pn instanceof ActivityNode) {
			return (ActivityNode) pn;
		}
		for (ProcessEdge edge : this.incomingEdgesOf(pn)) {
			ActivityNode actNode = getLastActivityNode(this.getEdgeSource(edge));
			if (actNode != null) {
				return actNode;
			}
		}
		return null;
	}
	
	private ActivityNode getFirstActivityNode(ProcessNode pn) {
		if (pn instanceof ActivityNode) {
			return (ActivityNode) pn;
		}
		for (ProcessEdge edge : this.outgoingEdgesOf(pn)) {
			ActivityNode actNode = getFirstActivityNode(this.getEdgeTarget(edge));
			if (actNode != null) {
				return actNode;
			}
		}
		return null;
	}

	@Override
	public AndSplit getContainingAndBlock(ActivityNode actNode) {
		for (ProcessNode node : this.vertexSet()) {
			if (node instanceof AndSplit) {
				if (this.andBlockContainsNode((AndSplit) node, actNode)) {
					return (AndSplit) node;
				}
			}
		}
		return null;
	}

	private boolean andBlockContainsNode(AndSplit split, ActivityNode actNode) {
		List<ProcessNode> queue = new ArrayList<ProcessNode>();
		
		for (ProcessEdge edge : this.outgoingEdgesOf(split)) {
			ProcessNode target = this.getEdgeTarget(edge);
			queue.add(target);
		}
		
		while (!queue.isEmpty()) {
			ProcessNode node = queue.remove(0);
			if (node instanceof ActivityNode && node.equals(actNode)) {
				return true;
			}
			if (!(node instanceof AndJoin)) {
				for (ProcessEdge edge : this.outgoingEdgesOf(node)) {
					queue.add(this.getEdgeTarget(edge));
				}
			}	
		}
		return false;
	}

	@Override
	public ActivityNode getFirstActivityNode() {
		for (ProcessNode pn : this.vertexSet()) {
			if (this.inDegreeOf(pn) == 0) {
				return getFirstActivityNode(pn);
			}
		}
		return null;
	}
	
	@Override
	public boolean shallowEquals(ProcessModel pm) {
		Map<ActivityNode, ActivityNode> correspondThis2PM = new HashMap<ActivityNode, ActivityNode>();
		Map<ActivityNode, ActivityNode> correspondPM2This = new HashMap<ActivityNode, ActivityNode>();
		
		Set<ActivityNode> thisNodes = new HashSet<ActivityNode>();
		for (ProcessNode node : this.vertexSet()) {
			if (node instanceof ActivityNode) {
				thisNodes.add((ActivityNode) node);
			} else {
				// comparing only simple processes, without connectivity nodes
				return false;
			}
		}
		
		Set<ActivityNode> pmNodes = new HashSet<ActivityNode>();
		for (ProcessNode node : pm.getProcessNodes()) {
			if (node instanceof ActivityNode) {
				pmNodes.add((ActivityNode) node);
			} else {
				return false;
			}
		}
		
		for (ActivityNode node1 : thisNodes) {
			for (ActivityNode node2 : pmNodes) {
				if (node1.getActivity().equals(node2.getActivity()) &&
						correspondPM2This.get(node2) == null) {
					correspondThis2PM.put(node1, node2);
					correspondPM2This.put(node2, node1);
					break;
				}
			}
		}
		for (ProcessNode node : thisNodes) {
			if (!correspondThis2PM.containsKey(node)) {
				return false;
			}
		}
		for (ProcessNode node : pmNodes) {
			if (!correspondPM2This.containsKey(node)) {
				return false;
			}
		}
		
		return shallowCompareEdges(this, pm, correspondThis2PM) 
				&& shallowCompareEdges(pm, this, correspondPM2This);
	}
	
	
	private boolean shallowCompareEdges(ProcessModel model1, ProcessModel model2, 
			Map<ActivityNode, ActivityNode> correspondence1To2) {
		
		for (ProcessEdge edge : model1.edgeSet()) {
			ActivityNode source = (ActivityNode) model1.getEdgeSource(edge);
			ActivityNode target = (ActivityNode) model1.getEdgeTarget(edge);
			if (!model2.containsEdge(correspondence1To2.get(source), correspondence1To2.get(target))) {
				return false;
			}
		}
		return true;
	}

	@Override
	public boolean containsProcessNode(ProcessNode node) {
		return this.containsVertex(node);
	}
	
}
