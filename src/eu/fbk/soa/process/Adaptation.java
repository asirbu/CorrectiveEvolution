package eu.fbk.soa.process;

import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import eu.fbk.soa.process.domain.DomainObject;
import eu.fbk.soa.process.node.ActivityNode;
import eu.fbk.soa.process.node.ProcessNode;

public class Adaptation {
	
	private static Logger logger = Logger.getLogger(Adaptation.class);

	private ProcessModel adaptModel;

	private ActivityNode from;

	private ActivityNode to;
	
	public Adaptation(ProcessModel adaptModel, ActivityNode from, ActivityNode to) {
		this.adaptModel = adaptModel;
		this.from = from;
		this.to = to;
	}


	public ActivityNode getFromNode() {
		return from;
	}


	public ActivityNode getToNode() {
		return to;
	}
	
	public StateFormula getPreconditionOfToNode() {
		return to.getActivity().getPrecondition();
	}

	public ProcessModel getAdaptationModel() {
		return adaptModel;
	}

	public boolean isApplicable(ProcessModel model, Trace trace) {
		if (trace.isEmpty()) {
			return isApplicable(model);
		}
		if (nodeConstraintsSatisfied(model)) {
			Set<ProcessNode> nodes = model.getProcessNodes(trace.getLastActivity());
			if (nodes.contains(from)) {
				return true;
			}
		}
		return false;
	}
	
	
	/*
	 * TODO to must not be part of an AND-block
	 */
	private boolean nodeConstraintsSatisfied(ProcessModel model) {
		// from, to are nodes in the original model
		if (!model.containsProcessNode(from) || !model.containsProcessNode(to)) {
			logger.error("Model does not contain the adaptation from/to node");
			return false;
		}
		
		// adaptation model does not contain nodes appearing 
		// in the original model
		for (ProcessNode node : adaptModel.getProcessNodes()) {
			if (model.containsProcessNode(node)) {
				logger.error("Adaptation model contains nodes appearing in original model");
				return false;
			}
		}
		return true;
	}
	
	public boolean isApplicable(ProcessModel model, List<Adaptation> adaptations, Trace trace) {
		return false;
	}

	/*
	 * TODO from must be reachable (there is a trace to from)
	 */
	public boolean isApplicable(ProcessModel model) {
		return nodeConstraintsSatisfied(model);
	}

	public String toString() {
		StringBuffer buffer = new StringBuffer();
		buffer.append("adaptation: from node ");
		buffer.append(from.getNodeID());
		buffer.append("(").append(from.getActivityName()).append(") to node ");
		buffer.append(to.getNodeID());
		buffer.append("(").append(to.getActivityName()); 
		buffer.append(") activities ");
		buffer.append(this.adaptModel.getProcessNodes().toString());

		return buffer.toString();
	}
	
	public boolean equals(Object obj) {
		if (!(obj instanceof Adaptation)) {
			return false;
		}
		Adaptation ad = (Adaptation) obj;
		return this.to.equals(ad.getToNode()) 
				&& this.from.equals(ad.getFromNode()) 
				&& this.adaptModel.shallowEquals(ad.getAdaptationModel());
	}


	public Set<DomainObject> getRelatedDomainObjects() {
		return this.adaptModel.getRelatedDomainObjects();
	}
}
