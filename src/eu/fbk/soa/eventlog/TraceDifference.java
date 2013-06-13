package eu.fbk.soa.eventlog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import eu.fbk.soa.evolution.Correction;
import eu.fbk.soa.evolution.Correction.Type;
import eu.fbk.soa.process.Activity;
import eu.fbk.soa.process.Adaptation;
import eu.fbk.soa.process.DefaultProcessModel;
import eu.fbk.soa.process.ProcessEdge;
import eu.fbk.soa.process.ProcessModel;
import eu.fbk.soa.process.StateFormula;
import eu.fbk.soa.process.Trace;
import eu.fbk.soa.process.domain.DomainObject;
import eu.fbk.soa.process.node.ActivityNode;
import eu.fbk.soa.process.node.EndNode;
import eu.fbk.soa.process.node.ProcessNode;
import eu.fbk.soa.process.node.XorJoin;
import eu.fbk.soa.process.node.XorSplit;
import eu.fbk.soa.util.CollectionsUtils;
import eu.fbk.soa.util.StringUtils;

public class TraceDifference {
	
	private static Logger logger = Logger.getLogger(TraceDifference.class);
	
	private static int nrDifferences = 0;
	
	private static int nrCorrections = 0;
	
	private List<String> executedActivities;

	private ActivityNode fromNode;

	private ActivityNode toNode;
	
	private List<String> activities;
	
	private int id;
	
	public TraceDifference(List<String> executedActs, List<String> actList, ActivityNode from) {
		nrDifferences++;
		id = nrDifferences;
		this.executedActivities = new ArrayList<String>(executedActs);
		this.fromNode = from;
		this.activities = new ArrayList<String>(actList);
	}
	
	
	public TraceDifference(List<String> executedActs, List<String> diffActs, ActivityNode fromNode,
			ActivityNode toNode) {
		this(executedActs, diffActs, fromNode);
		this.toNode = toNode;
	}

	public String getID() {
		return "D" + id;
	}


	public boolean equals(Object object) {
		if (!(object instanceof TraceDifference)) {
			return false;
		}
		TraceDifference diff = (TraceDifference) object;
		if (this.executedActivities.size() != diff.getExecutedActivities().size() ||
				this.activities.size() != diff.getActivities().size()) {
			return false;
		}
		if (!this.fromNode.equals(diff.getFromNode())) {
			return false;
		}
		
		ActivityNode otherToNode = diff.getToNode();
		if (toNode == null) {
			if (otherToNode != null) {
				return false;
			}
		} else {
			if (otherToNode == null && !this.toNode.equals(otherToNode)) {
				return false;
			}
		}
		
		for (int i = 0; i < this.executedActivities.size(); i++) {
			if (!executedActivities.get(i).equals(diff.getExecutedActivities().get(i))) {
				return false;
			}
		}
		for (int i = 0; i < this.activities.size(); i++) {
			if (!activities.get(i).equals(diff.getActivities().get(i))) {
				return false;
			}
		}
		return true;
	}

	public List<String> getExecutedActivities() {
		return executedActivities;
	}

	public ActivityNode getFromNode() {
		return fromNode;
	}

	public ActivityNode getToNode() {
		return toNode;
	}

	public List<String> getActivities() {
		return activities;
	}

	public void setExecutedActivities(List<String> executedActivities) {
		this.executedActivities = executedActivities;
	}


	public void setFromNode(ActivityNode fromNode) {
		this.fromNode = fromNode;
	}

	public void setToNode(ActivityNode toNode) {
		this.toNode = toNode;
	}
	
	
	public Correction generateRelaxedCorrection(int id, ProcessModel model, 
			Collection<Activity> allActivities) throws CorrectionGenerationException {
		
		List<Activity> acts = this.getMatchingActivities(activities, allActivities);
		
		fixTargetNode(model);
		StateFormula cond = this.generateCondition(model, acts);
		Adaptation adaptation = this.generateAdaptation(id, acts, fromNode);
		
		return new Correction(Type.RELAXED, new Trace(), cond, adaptation);
	}

	
	public List<Correction> generateCorrections(Type type, ProcessModel model, 
			Collection<Activity> allActivities) throws CorrectionGenerationException {
	
		List<Correction> corrections = new ArrayList<Correction>();
		List<Activity> acts = this.getMatchingActivities(activities, allActivities);
		List<List<Activity>> indepDiffActs = this.splitIntoIndependentDifferences(acts);
//		List<List<Activity>> indepDiffActs = this.dummySplitIntoIndependentDifferences(acts);
		
		List<Activity> tempExecActs = new ArrayList<Activity>();
		if (type.equals(Type.STRICT)) {
			List<Activity> execActs = this.getMatchingActivities(executedActivities, allActivities);	
			tempExecActs.addAll(execActs);
		}
		
		fixTargetNode(model);
		ActivityNode tempFromNode = fromNode;
		
		for (List<Activity> diffActs : indepDiffActs) {
			nrCorrections++;
			int id = nrCorrections; 			
			StateFormula cond = this.generateCondition(model, diffActs);
			Adaptation adaptation = this.generateAdaptation(id, diffActs, tempFromNode);
			
			Correction corr = new Correction(type, new Trace(tempExecActs), cond, adaptation);
			corrections.add(corr);
			
			tempFromNode = adaptation.getAdaptationModel().getLastActivityNode();
			if (type.equals(Type.STRICT)) {
				tempExecActs.addAll(diffActs);
			}
		}
		
		return corrections;			
	}
	
	private void fixTargetNode(ProcessModel model) throws CorrectionGenerationException {
		if (toNode == null) {
			for (ProcessNode node : model.getProcessNodes()) {
				if (node instanceof EndNode) {
					toNode = (ActivityNode) node;
					break;
				}
			}
		}
		if (toNode == null) {
			throw new CorrectionGenerationException(
					"Could not find the difference target node in the original model");
		}
	}
	
	private StateFormula generateCondition(ProcessModel model, List<Activity> acts) {
		StateFormula cond = new StateFormula();
		
		if (!acts.isEmpty()) {
			cond.add(acts.get(0).getPrecondition());
		} else {
			if (toNode != null) {
				cond.add(toNode.getActivity().getPrecondition());
			}
		}
		Set<DomainObject> domainObjects = cond.getRelatedDomainObjects();
		
		//TODO can there be multiple activity nodes at the next step? 
		for (ProcessEdge edge : model.outgoingEdgesOf(fromNode)) {
			ProcessNode target = model.getEdgeTarget(edge);
			
			if (target instanceof XorJoin) {
				if (model.outgoingEdgesOf(target).isEmpty()) {
					break;
				}
				ProcessEdge nextEdge = model.outgoingEdgesOf(target).iterator().next();
				target = model.getEdgeTarget(nextEdge);
			}
			if (target instanceof ActivityNode) {
				cond.add(getNegatedPrecondition((ActivityNode) target, domainObjects));
				break;
			} 
			if (target instanceof XorSplit) {
				for (ProcessEdge nextEdge : model.outgoingEdgesOf(target)) {
					ProcessNode branch = model.getEdgeTarget(nextEdge);
					if (branch instanceof XorJoin) {
						// there is a default branch!
						break;
					}
					if (branch instanceof ActivityNode) {
						cond.add(getNegatedPrecondition((ActivityNode) branch, domainObjects));
					}
				}
				break;
			}
		}
		
		logger.debug("Created correction condition " + cond);
		return cond;
	}

	private StateFormula getNegatedPrecondition(ActivityNode actNode, Set<DomainObject> domainObjects) {
		Activity act = actNode.getActivity();
		StateFormula prec = act.getPrecondition();
		StateFormula projection = prec.getProjection(domainObjects);
		if (!projection.isEmpty()) {
			return projection.getNegation();
		}
		return StateFormula.getTop();
	}
	
	private Adaptation generateAdaptation(int id, List<Activity> acts, ActivityNode fromNode) {
		ProcessModel adaptationModel = new DefaultProcessModel("Adaptation" + id);
		
		ActivityNode lastNode = null;
		for (Activity act : acts) {
			ActivityNode newActNode = new ActivityNode(act);
			adaptationModel.addNode(newActNode);
			if (lastNode != null) {
				adaptationModel.addEdge(lastNode, newActNode);
			} 
			lastNode = newActNode;
		}
		
		Adaptation adaptation = new Adaptation(adaptationModel, 
				fromNode, toNode);
		
		return adaptation;
	}
	
		
	private List<List<Activity>> dummySplitIntoIndependentDifferences(List<Activity> diffActs) {
		List<List<Activity>> indepDiffs = new ArrayList<List<Activity>>();
		indepDiffs.add(diffActs);
		return indepDiffs;
	}
	
	private List<List<Activity>> splitIntoIndependentDifferences(List<Activity> diffActs) {
		List<List<Activity>> indepDiffs = new ArrayList<List<Activity>>();
		if (diffActs.isEmpty()) {
			return indepDiffs;
		}
		
		List<Activity> currentList = new ArrayList<Activity>();
		Activity firstAct = diffActs.get(0);
		currentList.add(firstAct);
		
		Set<DomainObject> currentObjects = new HashSet<DomainObject>();
		currentObjects.addAll(firstAct.getRelatedDomainObjects());
		
		for (int i = 1; i < diffActs.size(); i++) {
			Activity act = diffActs.get(i);
			Set<DomainObject> newObjects = act.getRelatedDomainObjects();
			if (currentObjects.isEmpty() || newObjects.isEmpty() 
					|| CollectionsUtils.nonEmptyIntersection(currentObjects, newObjects)) {
				currentList.add(act);
				currentObjects.addAll(newObjects);
//				logger.info("Added to the same list activity " + act);
			} else {
				indepDiffs.add(currentList);
//				logger.info("Starting a different list for activity " + act);
				currentList = new ArrayList<Activity>();
				currentList.add(act);
				currentObjects.clear();
				currentObjects.addAll(newObjects);
			}	
		}
		indepDiffs.add(currentList);
		return indepDiffs;
	}

	
	private List<Activity> getMatchingActivities(List<String> actNames, Collection<Activity> actList) 
	throws CorrectionGenerationException {
	
		List<Activity> acts = new ArrayList<Activity>();
		for (String actName : actNames) {
			Activity matchingAct = null;
			for (Activity act : actList) {
				if (act.getName().equals(actName)) {
					matchingAct = act;
					break;
				}
			}
			if (matchingAct != null) {
				acts.add(matchingAct);
			} else {
				throw new CorrectionGenerationException("Logged activity " + 
						actName + " does not appear among the declared activities");
			}
		}
		return acts;
	}

	public boolean relaxedEquals(TraceDifference diff) {
		
		if (this.activities.size() != diff.getActivities().size()) {
			return false;
		}
		if (!this.fromNode.equals(diff.getFromNode())) {
			return false;
		}
		if (this.toNode == null || diff.getToNode() == null) {
			if (!(this.toNode == null && diff.getToNode() == null)) {
				return false;
			} 
		} else { 
			if (!this.toNode.equals(diff.getToNode())) {
				return false;
			}
		}
		for (int i = 0; i < this.activities.size(); i++) {
			if (!activities.get(i).equals(diff.getActivities().get(i))) {
				return false;
			}
		}
		return true;
	}

	
	public String getShortDescr() {
		return getStringRepresentation(false);
	}
	
	public String toString() {
		return getStringRepresentation(true);
	}
	
	private String getStringRepresentation(boolean includeTrace) {
		StringBuffer buffer = new StringBuffer("difference ");
		
		buffer.append(this.getID());
		if (includeTrace) {
			buffer.append("\n\t - executed trace: {");
			buffer.append(StringUtils.getCommaSeparatedString(executedActivities));
			buffer.append("}");
		}
		buffer.append("\n\t - from node: ");
		buffer.append(fromNode.getNodeID());
		
		if (toNode != null) {
			buffer.append(" to node: ");
			buffer.append(toNode.getNodeID());
		}
		buffer.append("\n\t - activities: "); 
		buffer.append(StringUtils.getCommaSeparatedString(activities));
		
		return buffer.toString();
	}
	
}
