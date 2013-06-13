package eu.fbk.soa.eventlog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import eu.fbk.soa.process.Activity;
import eu.fbk.soa.process.ProcessEdge;
import eu.fbk.soa.process.ProcessModel;
import eu.fbk.soa.process.domain.DomainObject;
import eu.fbk.soa.process.node.ActivityNode;
import eu.fbk.soa.process.node.AndJoin;
import eu.fbk.soa.process.node.AndSplit;
import eu.fbk.soa.process.node.ProcessNode;
import eu.fbk.soa.util.CollectionsUtils;

public class TraceReplay {
	
	private static Logger logger = Logger.getLogger(TraceReplay.class);

	private static Integer maxTraceLength = 30;
	
	private ProcessModel model;
	
	private Set<Activity> allActivities;
	
	private Map<String, Activity> activitiesByName;
	
	
	public TraceReplay(ProcessModel processModel, Set<Activity> activities) {
		this.model = processModel;
		this.allActivities = activities;
		this.activitiesByName = new HashMap<String, Activity>();
		for (Activity act : allActivities) {
			activitiesByName.put(act.getName(), act);
		}
	}
	
	
	public List<TraceDifference> computeDifferences(List<String> activities) {
		List<TraceDifference> differences = new ArrayList<TraceDifference>();
		if (activities.isEmpty()) {
			return differences;
		}
		
		List<String> executedActivities = new ArrayList<String>();
		List<String> unmatchedActs = new ArrayList<String>(activities);
				
		List<String> diffActs = new ArrayList<String>();
		
		// TODO it may be the case that the trace does not start with the start node!
		ActivityNode lastMatchedNode = null;
		
		while (!unmatchedActs.isEmpty()) {
			String firstAct = unmatchedActs.remove(0);
			ActivityNode node = matchNextActivityFromNode(firstAct, lastMatchedNode);
			
			if (node != null) {
				if (!diffActs.isEmpty()) {
					differences.add(
							generateTraceDifference(diffActs, executedActivities, lastMatchedNode, node));					
					diffActs = new ArrayList<String>();
				} 
				executedActivities.add(firstAct);
				lastMatchedNode = node;
			} else {
				diffActs.add(firstAct);
			}
		}
		if (!diffActs.isEmpty()) {
			differences.add(
					generateTraceDifference(diffActs, executedActivities, lastMatchedNode, null));
		}
		return differences;
	}
	
	
	private ActivityNode matchNextActivityFromNode(String act, ActivityNode lastMatchedNode) {
		List<ProcessNode> nextNodes = new ArrayList<ProcessNode>();
		if (lastMatchedNode == null) {
			nextNodes.add(model.getStartNode());
		} else {
			// matching the first node back 
			if (lastMatchedNode.getActivityName().equals(act)) {
				return lastMatchedNode;
			}

			for (ProcessEdge edge : model.outgoingEdgesOf(lastMatchedNode)) {
				nextNodes.add(model.getEdgeTarget(edge));
			}
			
			nextNodes.addAll(getNextActivitiesInAndBlock(lastMatchedNode));
		}
		return this.matchActivity(act, nextNodes);
	}


	private List<ProcessNode> getNextActivitiesInAndBlock(ActivityNode lastMatchedNode) {
		List<ProcessNode> nextNodes = new ArrayList<ProcessNode>();
		
		AndSplit matchingAndSplit = model.getContainingAndBlock(lastMatchedNode);
		if (matchingAndSplit == null) {
			return nextNodes;
		}
		
		for (ProcessEdge edge : model.outgoingEdgesOf(matchingAndSplit)) {	
			List<ProcessNode> branchNodes = new ArrayList<ProcessNode>();
			
			ProcessNode node = model.getEdgeTarget(edge);
			boolean branchContinues = true;
			while (branchContinues && !(node instanceof AndJoin)) {
				if (node.equals(lastMatchedNode)) {
					branchNodes.clear();
					for (ProcessEdge nextEdge : model.outgoingEdgesOf(node)) {
						branchNodes.add(model.getEdgeTarget(nextEdge));
					}
					branchContinues = false;
				} else {
					branchNodes.add(node);
					Set<ProcessEdge> nextEdges = model.outgoingEdgesOf(node);
					if (nextEdges.size() > 1) {
						throw new UnsupportedOperationException("Process models currently cannot contain " +
								"control connectors in And-blocks.");
					}
					Iterator<ProcessEdge> edgeIterator = nextEdges.iterator();
					if (edgeIterator.hasNext()) {
						node = model.getEdgeTarget(edgeIterator.next());
					} else {
						branchContinues = false;
					}
				}
			}
			nextNodes.addAll(branchNodes);			
		}

		return nextNodes;
	}
	
	
	private TraceDifference generateTraceDifference(List<String> diffActs, List<String> executedActivities, 
			ActivityNode fromNode, ActivityNode toNode) {
		TraceDifference newDiff = new TraceDifference(executedActivities, diffActs, fromNode, toNode);
		executedActivities.addAll(diffActs);
		return newDiff;
	}
	
	private List<List<String>> splitIntoIndependentDifferences(List<String> diffActs) {
		
		List<List<String>> indepDiffs = new ArrayList<List<String>>();
		if (diffActs.isEmpty()) {
			return indepDiffs;
		}
		
		List<String> currentList = new ArrayList<String>();
		String firstAct = diffActs.get(0);
		currentList.add(firstAct);
		
		Set<DomainObject> currentObjects = new HashSet<DomainObject>();
//		logger.info("Added activity " + firstAct);
		currentObjects.addAll(this.activitiesByName.get(firstAct).getRelatedDomainObjects());

		for (int i = 1; i < diffActs.size(); i++) {
			String act = diffActs.get(i);
			Set<DomainObject> newObjects = this.activitiesByName.get(act).getRelatedDomainObjects();
			if (currentObjects.isEmpty() || newObjects.isEmpty() || CollectionsUtils.nonEmptyIntersection(currentObjects, newObjects)) {
				currentList.add(act);
				currentObjects.addAll(newObjects);
//				logger.info("Added to the same list activity " + act);
				
			} else {
				indepDiffs.add(currentList);
//				logger.info("Starting a different list for activity " + act);
				
				currentList = new ArrayList<String>();
				currentList.add(act);
				currentObjects.clear();
				currentObjects.addAll(newObjects);
			}	
		}
		indepDiffs.add(currentList);
		return indepDiffs;
	}
	

	private ActivityNode matchActivity(String act, List<ProcessNode> nextNodes) {
		if (nextNodes.isEmpty()) {
			return null;
		}
		
		Set<ProcessNode> toRemove = new HashSet<ProcessNode>();	
		Set<ProcessNode> toReplace = new HashSet<ProcessNode>();
		
		for (ProcessNode node : nextNodes) {
			if (node instanceof ActivityNode) {
				ActivityNode actNode = (ActivityNode) node;
				if (actNode.getActivityName().equals(act)) {
					return actNode;				} 
			} else {
				toReplace.add(node);
			}
			toRemove.add(node);
		}
		
		nextNodes.removeAll(toRemove);
		for (ProcessNode node : toReplace) {
			for (ProcessEdge edge : model.outgoingEdgesOf(node)) {
				nextNodes.add(model.getEdgeTarget(edge));
			}
		}
		
		return matchActivity(act, nextNodes);
	}
		
}
