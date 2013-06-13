package eu.fbk.soa.eventlog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import eu.fbk.soa.process.node.ActivityNode;
import eu.fbk.soa.process.node.ProcessNode;

public class CorrectionGenerator {

	static Logger logger = Logger.getLogger(CorrectionGenerator.class);
	
	private LogExplorer logExplorer;
	
	private DifferenceAnalysis diffExplorer;
	
	private Set<Activity> allActivities;
	
	private ProcessModel model;
	
	private boolean strictMode = true;
	
	public CorrectionGenerator(ProcessModel model, Set<Activity> activities, LogExplorer explorer) {
		this.model = model;
		this.allActivities = activities;
		
		this.logExplorer = explorer;
		this.diffExplorer = new DifferenceAnalysis(model, allActivities, logExplorer);
	}
	
	public List<Correction> generateRelevantStrictCorrections() throws CorrectionGenerationException {
		this.setStrictMode(true);
		
		Map<TraceDifference, Double> relevantDiffs = diffExplorer.computeRelevantDifferences();
		
		List<TraceDifference> orderedDiffs = diffExplorer.orderByExecutedTraceSize(
				new ArrayList<TraceDifference>(relevantDiffs.keySet()));
			
		List<Correction> corrections = new ArrayList<Correction>();
		Map<Correction, Double> percentages = new HashMap<Correction, Double>();
		
		for (int i = 0; i < orderedDiffs.size(); i++) {
			TraceDifference diff = orderedDiffs.get(i);
			List<Correction> newCorrections = diff.generateCorrections(Type.STRICT, model, allActivities);
			
			for (Correction corr : newCorrections) {
				logger.info("Generated strict correction from difference " + diff.getID() + ":\n\t" + corr);
				
				Correction identical = null;
				for (Correction existingCorr : corrections) {
					if (existingCorr.equals(corr)) {
						identical = existingCorr;
						break;
					}
				}
				if (identical != null) {
					logger.info("Correction " + corr.getName() 
							+ " is identical to previously generated correction " 
							+ identical.getName() + ", skipping " + corr.getName());
				} else {
					percentages.put(corr, relevantDiffs.get(diff));
					corrections.add(corr);
				}
			}
		}
		
		List<Correction> updatedCorrections = this.removeOverlaps(corrections, percentages);
		for (Correction c : updatedCorrections) {
			logger.info("Final " + c);
		}
		return updatedCorrections;
	}
	
	
	public List<TraceDifference> getDifferencesWithTraces(Map<TraceDifference, Double> diffsWithOccurence) {
		this.setStrictMode(true);
		
		diffsWithOccurence.putAll(diffExplorer.computeRelevantDifferences());
		
		List<TraceDifference> orderedDiffs = diffExplorer.orderByExecutedTraceSize(
				new ArrayList<TraceDifference>(diffsWithOccurence.keySet()));
		return orderedDiffs;
	}
	
	public List<TraceDifference> getDifferencesWithoutTraces(Map<TraceDifference, Double> diffsWithOccurence) {
		this.setStrictMode(false);
		
		diffsWithOccurence.putAll(diffExplorer.computeRelevantDifferences());
		
		List<TraceDifference> orderedDiffs = diffExplorer.orderByExecutedTraceSize(
				new ArrayList<TraceDifference>(diffsWithOccurence.keySet()));
		return orderedDiffs;
	}
	
	
	public List<Correction> generateRelevantRelaxedCorrections(Map<TraceDifference, Double> relevantDiffs, 
			List<TraceDifference> orderedDiffs) throws CorrectionGenerationException {
		this.setStrictMode(false);
		List<Correction> corrections = new ArrayList<Correction>();
		
		Map<Correction, Double> percentages = new HashMap<Correction, Double>();
		for (TraceDifference diff : orderedDiffs) {	
			Map<Correction, Correction> matches = new HashMap<Correction, Correction>();
			List<Correction> newCorrections = diff.generateCorrections(Type.RELAXED, model, allActivities);
			
			for (Correction corr : newCorrections) {
				logger.info("Generated relaxed correction from difference " + diff.getID() + ":\n\t" + corr);
				
				Correction identical = null;
				for (Correction existingCorr : corrections) {
					if (existingCorr.equals(corr)) {
						identical = existingCorr;
						matches.put(corr, existingCorr);
						break;
					}
					if (corr.isSameExceptForFromNode(existingCorr)) {
						ActivityNode fromNode1 = corr.getAdaptation().getFromNode();
						ActivityNode fromNode2 = existingCorr.getAdaptation().getFromNode();
						
						for (Correction c1 : matches.keySet()) {
							if (c1.getAdaptation().getAdaptationModel().containsProcessNode(fromNode1)) {
								Correction c2 = matches.get(c1);
								if (c2.getAdaptation().getAdaptationModel().containsProcessNode(fromNode2)) {
									identical = existingCorr;
									matches.put(corr, existingCorr);
									break;
								}
							}
						}
						if (identical != null) {
							break;
						}
					}
					// to test if they are the same except for the from node, and the from node corresponds to identical models
				}
				if (identical != null) {
					logger.info("Correction " + corr.getName() 
							+ " is identical to previously generated correction " 
							+ identical.getName() + ", skipping " + corr.getName());
				} else {
					percentages.put(corr, relevantDiffs.get(diff));
					corrections.add(corr);
				}
			}
		}
		
		
		logger.info("Removing overlaps between corrections...");
		
		List<Correction> updatedCorrections = this.removeOverlaps(corrections, percentages);
		for (Correction c : updatedCorrections) {
			logger.info("Final " + c);
		}
		return updatedCorrections;
	}
	

	private void setStrictMode(boolean newValue) {
		this.strictMode = newValue;
		diffExplorer.setStrictComparisons(newValue);
	}
	
	
	private List<Correction> removeOverlaps(List<Correction> corrections, 
			Map<Correction, Double> percentages) {
		
		if (corrections.size() <= 1) {
			return corrections;
		}
		logger.info("Removing overlaps between corrections...");
		
		boolean updates = false;
		List<Correction> correctionsCopy = new ArrayList<Correction>(corrections);
		
		for (int i = 0; i < corrections.size(); i++) {
			Correction corr1 = corrections.get(i);
			Double p1 = percentages.get(corr1);

			for (int j = i + 1; j < corrections.size(); j++) {
				Correction corr2 = corrections.get(j);
				Double p2 = percentages.get(corr2);

				if (corr1.hasSameSetting(corr2)) {
					updates = true;
					logger.info("Found overlap between correction " + 
							corr1.getName() + " (frequency " + String.format("%.2f", p1) + "%) and " + 
							"correction " + corr2.getName() + " (frequency " + String.format("%.2f", p2) + "%)");
					
					Correction moreFrequent = corr1;
					Correction lessFrequent = corr2;
					Double lessFrequentPercentage = p2;
					
					if (Double.compare(p1, p2) < 0) {
						moreFrequent = corr2;
						lessFrequent = corr1;
						lessFrequentPercentage = p1;
					} 
					correctionsCopy.remove(lessFrequent);	
					percentages.remove(lessFrequent);
					
					if (!compatible(corr1, corr2)) {
						logger.info("Corrections are incompatible, removed the less frequent");
						
					} else {
						logger.info("Corrections are compatible, updating the less frequent..");
						Correction corr = updateCorrection(lessFrequent, moreFrequent);
						correctionsCopy.add(corr);
						percentages.put(corr, lessFrequentPercentage);
					}
					break;
				}
			}
			if (updates) {
				break;
			}
		}
		
		if (updates) {
			return removeOverlaps(correctionsCopy, percentages);
		}
		return corrections;
	}

	
	
	
	private Correction updateCorrection(Correction lessFrequent, Correction moreFrequent) {
		
		Adaptation lessFreqAd = lessFrequent.getAdaptation();
		Adaptation moreFreqAd = moreFrequent.getAdaptation();
		
		ProcessModel lessFreqModel = lessFreqAd.getAdaptationModel();
		ProcessModel moreFreqModel = moreFreqAd.getAdaptationModel();
		
		ProcessNode lastMatchingNode1 = this.getFirstNode(lessFreqModel);
		ProcessNode lastMatchingNode2 = this.getFirstNode(moreFreqModel);
		
		Trace newTrace = new Trace(lessFrequent.getTrace().getActivities());
		
		boolean similar = true;
		boolean finishedLessFreq = false;
		
		
		while (similar) {
			Activity act = ((ActivityNode) lastMatchingNode2).getActivity();
			newTrace.addActivity(act);
			
			Set<ProcessEdge> edges1 = new HashSet<ProcessEdge>();
			if (lastMatchingNode1 != null) {
				edges1 = lessFreqModel.outgoingEdgesOf(lastMatchingNode1);
			}
			Set<ProcessEdge> edges2 = new HashSet<ProcessEdge>();
			if (lastMatchingNode2 != null) {
				edges2 = moreFreqModel.outgoingEdgesOf(lastMatchingNode2);
			}
			
			if (edges1.size() == 0 || edges2.size() == 0) {
				if (edges1.size() == 0) {
					finishedLessFreq = true;
				} 
				break;
			}
			if (edges1.size() > 1 || edges2.size() > 1) {
				// TODO should be able to compare also structures, not only sequences! 
				logger.info("More then one outgoing edge");
				return null;
			}
			
			ProcessNode node1 = lessFreqModel.getEdgeTarget(edges1.iterator().next());
			ProcessNode node2 = moreFreqModel.getEdgeTarget(edges2.iterator().next());
			
			similar = similar(node1, node2);
			if (similar) {
				lastMatchingNode1 = node1;
				lastMatchingNode2 = node2;
			}
		}
		
		ProcessModel newModel = new DefaultProcessModel("Updated" + lessFreqModel.getName());
		ActivityNode newFromNode = (ActivityNode) lastMatchingNode2;
		
		Set<ProcessEdge> edges = moreFreqModel.outgoingEdgesOf(lastMatchingNode2);
		StateFormula newCondition = new StateFormula();
		if (edges.isEmpty()) {
			newCondition = moreFreqAd.getPreconditionOfToNode().getNegation();
		} else {
			for (ProcessEdge edge : edges) {
				ProcessNode nextNode = moreFreqModel.getEdgeTarget(edge);
				if (nextNode instanceof ActivityNode) {
					ActivityNode actNextNode = (ActivityNode) nextNode ;
					Activity act = actNextNode.getActivity();
					newCondition.add(act.getPrecondition().getNegation());
				}
			}
		}
				
		if (!finishedLessFreq) {
			copyRemainingActivityNodes(lessFreqModel, lastMatchingNode1, newModel);
			
			if (newModel.getFirstActivityNode() != null) {
				Activity act = newModel.getFirstActivityNode().getActivity();
				newCondition.add(act.getPrecondition());
			}
		}
		
		Adaptation newAdaptation = new Adaptation(newModel, newFromNode, lessFreqAd.getToNode());
		Correction newCorr = null;
		if (this.strictMode) {
			newCorr = new Correction(lessFrequent.getType(), newTrace, newCondition, newAdaptation); 
		} else {
			newCorr = new Correction(lessFrequent.getType(), new Trace(), newCondition, newAdaptation);
		}
		return newCorr;
	}
	
	
	private void copyRemainingActivityNodes(ProcessModel sourceModel, ProcessNode lastNode, 
			ProcessModel destinationModel) {
		ProcessNode currentNode = lastNode;
		boolean finished = false;
		
		while (!finished) {
			Set<ProcessEdge> edges1 = new HashSet<ProcessEdge>();
			if (currentNode != null) {
				edges1 = sourceModel.outgoingEdgesOf(currentNode);
			}
			if (edges1.isEmpty()) {
				finished = true;
				break;
			}
			ProcessNode node = sourceModel.getEdgeTarget(edges1.iterator().next());
			destinationModel.addNode(node);
			if (!currentNode.equals(lastNode)) {
				destinationModel.addEdge(currentNode, node);
			}
			currentNode = node;
		}
		
	}

	private boolean compatible(Correction corr1, Correction corr2) {
		Adaptation ad1 = corr1.getAdaptation();
		Adaptation ad2 = corr2.getAdaptation();
		
		ProcessNode currentNode1 = this.getFirstNode(ad1.getAdaptationModel());
		ProcessNode currentNode2 = this.getFirstNode(ad2.getAdaptationModel());
		
		return (similar(currentNode1, currentNode2));
	}
	
	
	
	
	private boolean similar(ProcessNode node1, ProcessNode node2) {
		if (node1 == null || node2 == null) {
			// an empty adaptation is compatible with any other adaptation
			return true;
		}
		
		if (node1 instanceof ActivityNode && node2 instanceof ActivityNode) {
			ActivityNode actNode1 = (ActivityNode) node1;
			ActivityNode actNode2 = (ActivityNode) node2;
			if (actNode1.getActivity().equals(actNode2.getActivity())) {
				return true;
			} else {
				return false;
			}
		}
		// TODO the nodes can be similar also if they are XorSplit/Join, AndSplit/Join
		return false;
	}

	private ProcessNode getFirstNode(ProcessModel model) {
		Set<ProcessNode> nodeSet = model.getProcessNodes();
		
		for (ProcessNode node : nodeSet) {
			if (model.inDegreeOf(node) == 0) {
				return node;
			}
		}
		return null;
	}

	
}
