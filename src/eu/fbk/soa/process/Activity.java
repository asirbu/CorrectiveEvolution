package eu.fbk.soa.process;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;

import eu.fbk.soa.process.domain.DomainObject;
import eu.fbk.soa.process.node.ActivityNode;
import eu.fbk.soa.process.node.AndJoin;
import eu.fbk.soa.process.node.AndSplit;
import eu.fbk.soa.process.node.ProcessNode;

@XmlAccessorType(XmlAccessType.NONE)
public class Activity {

	private static Set<Activity> activities = new HashSet<Activity>();
	
	@XmlAttribute(name="name", required=true)
	private String name;
	
	@XmlElement(name = "precondition", namespace = "http://soa.fbk.eu/Process")
	private StateFormula precondition;
	
	@XmlElement(name = "effect", namespace = "http://soa.fbk.eu/Process")
	private Effect effects;
	
	public Activity(){
		this.precondition = new StateFormula();
		this.effects = new Effect();
	}
	
	public Activity(String activityName, StateFormula prec, Effect eff) {
		this.name = activityName;
		this.precondition = prec;
		this.effects = eff;	
	}

	public String getName() {
		return name;
	}
		
	public static Activity getActivity(String name) {
		return Activity.getActivity(name, new StateFormula(), new Effect());
	}
	
	public static Activity getActivity(String name, StateFormula prec, Effect eff) {
		for (Activity act : activities) {
			if (act.matches(name, prec, eff)) {
				return act;
			}
		}
		Activity newAct = new Activity(name, prec, eff);
		activities.add(newAct);
		return newAct;
	}
	
	public boolean matches(String actName, StateFormula prec, Effect eff) {
		return (name.equals(actName) && precondition.equals(prec) && effects.equals(eff));
	}

	public boolean equals(Object obj) {
		if (obj instanceof Activity) {
			Activity act = (Activity) obj;
			return this.name.equals(act.getName()) && 
				this.precondition.equals(act.getPrecondition()) &&
				this.effects.equals(act.getEffect());
		}
		return false;
	}
	
	public String toString() {
		return this.name;
	}
	
	public Set<DomainObject> getRelatedDomainObjects() {
		Set<DomainObject> objects = new HashSet<DomainObject>();
		objects.addAll(precondition.getRelatedDomainObjects());
		objects.addAll(effects.getRelatedDomainObjects());
		return objects;
	}

	public StateFormula getPrecondition() {
		return this.precondition;
	}
	
	public boolean appearsAsNode(ProcessModel model) {
		Set<ProcessNode> nodes = model.getProcessNodes(this);
		return (!nodes.isEmpty());
	}

	public Effect getEffect() {
		return this.effects;
	}
	
	public void updateObjectReferences(Set<DomainObject> objects) {
		this.precondition.updateObjectReferences(objects);
		this.effects.updateObjectReferences(objects);

	}
	
	public boolean hasUnresolvedReferences() {
		return this.precondition.hasUnresolvedReferences() &&
			this.effects.hasUnresolvedReferences();
	}

	public boolean isLastNode(ProcessModel model) {
		
		for (ProcessNode node : model.getProcessNodes()) {
			if (node instanceof ActivityNode) {
				ActivityNode actNode = (ActivityNode) node;
				if (actNode.getActivity().equals(this) && model.outgoingEdgesOf(node).isEmpty()) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean followsDirectly(Activity prevAct, ProcessModel model) {
		boolean follows = false;
		for (ProcessNode node : model.getProcessNodes()) {
			if (node instanceof ActivityNode) {
				ActivityNode actNode = (ActivityNode) node;
				if (actNode.getActivity().equals(this)) {
					follows = findActivityInPreviousNodes(prevAct, actNode, model); 
					if (follows) {
						break;
					}
				}
				AndSplit andSplit = model.getContainingAndBlock(actNode);
				if (andSplit != null) {
					follows = findActivityInAndBlock(prevAct, actNode, andSplit, model);
					if (follows) {
						break;
					}
				}
			}
		}
		
		
		return follows;
	}

	private boolean findActivityInAndBlock(Activity prevAct, ActivityNode actNode, AndSplit andSplit, ProcessModel model) {
		List<ProcessNode> differentBranchNodes = new ArrayList<ProcessNode>();
		for (ProcessEdge edge : model.outgoingEdgesOf(andSplit)) {
			List<ProcessNode> branchNodes = new ArrayList<ProcessNode>();
			boolean foundOnBranch = false;
			
			while (!foundOnBranch) {
				ProcessNode next = model.getEdgeTarget(edge);
				if (next instanceof AndJoin) {
					break;
				} 
				if (next.equals(actNode)) {
					foundOnBranch = true;
				} else {
					branchNodes.add(next);
				}
				edge = model.outgoingEdgesOf(next).iterator().next();
			}
			if (!foundOnBranch) {
				differentBranchNodes.addAll(branchNodes);
			}
		}
		while (!differentBranchNodes.isEmpty()) {
			ProcessNode prev = differentBranchNodes.remove(0);
			if (prev instanceof ActivityNode) {
				ActivityNode actPrev = (ActivityNode) prev;
				if (actPrev.getActivity().equals(prevAct)) {
					return true;
				}
			} 
		}
		
		return false;
	}
	
	private boolean findActivityInPreviousNodes(Activity prevAct, ActivityNode actNode, ProcessModel model) {
		List<ProcessNode> previousNodes = new ArrayList<ProcessNode>();
		for (ProcessEdge edge : model.incomingEdgesOf(actNode)) {
			ProcessNode prev = model.getEdgeSource(edge);
			previousNodes.add(prev);
		}
		while (!previousNodes.isEmpty()) {
			ProcessNode prev = previousNodes.remove(0);
			if (prev instanceof ActivityNode) {
				ActivityNode actPrev = (ActivityNode) prev;
				if (actPrev.getActivity().equals(prevAct)) {
					return true;
				}
			} else {
				for (ProcessEdge edge : model.incomingEdgesOf(prev)) {
					previousNodes.add(model.getEdgeSource(edge));
				}
			}
		}
		return false;      
	}
	
	public boolean isFirstNode(ProcessModel model) {
		for (ProcessNode node : model.getProcessNodes()) {
			if (node instanceof ActivityNode) {
				ActivityNode actNode = (ActivityNode) node;
				if (actNode.getActivity().equals(this) && model.incomingEdgesOf(node).isEmpty()) {
					return true;
				}
			}
		}
		return false;
	}
	
}
