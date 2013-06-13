package eu.fbk.soa.eventlog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import eu.fbk.soa.process.Activity;
import eu.fbk.soa.process.ProcessModel;


public class DifferenceAnalysis {

	private static Logger logger = Logger.getLogger(DifferenceAnalysis.class);
	
	public static Double minPercentageThreshold = 14.0;
	
	public static Double maxPercentageThreshold = 100.0;
	
	
	private static Double shownPercentageThreshold = 4.0;
	
	private boolean strictComparisons = true;
	
	private Map<String, List<String>> traceTypes;
	
	private Set<String> divergingTraces; 
	
	private Map<TraceDifference, Set<String>> differenceOccurences;
	
	private TraceReplay replay;
	
	private Map<String, Integer> nrOfTracesByType;
	
	private int totalNrTraces;
	
	
	public DifferenceAnalysis(ProcessModel model, Set<Activity> activities, LogExplorer explorer) {
		
		this.replay = new TraceReplay(model, activities);
		this.divergingTraces = explorer.getDivergingTraces();
		this.totalNrTraces = explorer.getTotalNrTraces();
		this.nrOfTracesByType = explorer.getNrOfTracesByType();
		this.traceTypes = explorer.getTraceTypes();
		
		differenceOccurences = new HashMap<TraceDifference, Set<String>>();
	}
	
	
	public Map<TraceDifference, Double> computeRelevantDifferences() {
		logger.info("Computing differences..");
		
		for (String traceID : this.divergingTraces) {
			List<String> activities = this.traceTypes.get(traceID);
			List<TraceDifference> differences = replay.computeDifferences(activities);
			addDifferenceOccurrences(differences, traceID);	
		}
		Map<TraceDifference, Double> relevantDiffs = collectFrequentDifferences();
		return relevantDiffs;
	}
	
	
	
	private void addDifferenceOccurrences(List<TraceDifference> differences, String traceID) {
		
		for (TraceDifference difference : differences) {
			TraceDifference matchingDiff = null;
			for (TraceDifference diff : this.differenceOccurences.keySet()) {
				if (this.strictComparisons && diff.equals(difference) ||
						(!this.strictComparisons) && diff.relaxedEquals(difference)) {
					matchingDiff = diff;
					break;
				}
			}
			Set<String> traces = new HashSet<String>();
			traces.add(traceID);

			if (matchingDiff != null) {
				traces.addAll(differenceOccurences.get(matchingDiff));
				this.differenceOccurences.put(matchingDiff, traces);
			} else {
				this.differenceOccurences.put(difference, traces);
			}
		}
	}
	
	private Map<TraceDifference, Double> collectFrequentDifferences() {
//		List<TraceDifference> relevant = new ArrayList<TraceDifference>();
				
		Map<TraceDifference, Double> greaterThenMinThreshold = new HashMap<TraceDifference, Double>();
		Map<Double, TraceDifference> greaterThenDisplayMinimum = new HashMap<Double, TraceDifference>();
		Map<TraceDifference, Integer> nrOfInstances = new HashMap<TraceDifference, Integer>();
		
		
		for (TraceDifference diff : this.differenceOccurences.keySet()) {
			Set<String> traces = differenceOccurences.get(diff);
			
			Integer nrInstances = 0;
			for (String tID : traces) {
				nrInstances += this.nrOfTracesByType.get(tID);
			}
			Double percentage = new Double(nrInstances);
			percentage /= this.totalNrTraces;
			percentage *= 100;
			if (percentage > shownPercentageThreshold) {
				greaterThenDisplayMinimum.put(percentage, diff);
				nrOfInstances.put(diff, nrInstances);
			}
			if (Double.compare(percentage, minPercentageThreshold) >= 0 &&
					Double.compare(percentage, maxPercentageThreshold) <= 0) {
					greaterThenMinThreshold.put(diff, percentage);
			}    
		}
		
		logger.info("Printing differences occuring for more then " + 
				shownPercentageThreshold + "% of the traces");
		printDifferences(greaterThenDisplayMinimum, nrOfInstances);
		
		logger.warn(greaterThenMinThreshold.keySet().size() + " differences considered");
		
//		List<Double> relevantPercentages = this.getDecreasingOrderList(greaterThenThreshold.keySet());
//		for (int i = 0; i < relevantPercentages.size(); i++) {
//			relevant.add(greaterThenThreshold.get(relevantPercentages.get(i)));
//		}
		return greaterThenMinThreshold;
	}
	
	private void printDifferences(Map<Double, TraceDifference> differencesByPercentage,
			Map<TraceDifference, Integer> nrOfInstances) {
		
		List<Double> decrOrderPercentages = this.getDecreasingOrderList(differencesByPercentage.keySet());
		for (int i = 0; i < decrOrderPercentages.size(); i++) {
			Double percentage = decrOrderPercentages.get(i);
			TraceDifference diff = differencesByPercentage.get(percentage);
			
			String diffStr = "";
			if (this.strictComparisons) {
				diffStr = diff.toString();
			} else {
				diffStr = diff.getShortDescr();
			}
			logger.info(String.format("%.2f", percentage) +
					"% (" + nrOfInstances.get(diff) + ") traces contain " + diffStr);
			List<String> example = this.traceTypes.get(
					differenceOccurences.get(diff).iterator().next());
			logger.info("Example trace " + example);
		}
		
	}
	
	
	private List<Double> getDecreasingOrderList(Set<Double> set) {
		List<Double> decrOrderList = new ArrayList<Double>();
		for (Double elem : set) {
			int index = decrOrderList.size();
			for (int i = 0; i < decrOrderList.size(); i++) {
				if (Double.compare(elem, decrOrderList.get(i)) >= 0) {
					index = i;
					break;
				}
			}
			decrOrderList.add(index, elem);	
		}
		return decrOrderList;
	}


	public void setStrictComparisons(boolean b) {
		this.strictComparisons = b;
	}

	
	/*
	 * implements quicksort 
	 */
	public List<TraceDifference> orderByExecutedTraceSize(List<TraceDifference> relevantDiffs) {
		if (relevantDiffs.size() <= 1) {
			return relevantDiffs;
		}
		
		List<TraceDifference> ordered = new ArrayList<TraceDifference>();
		
		TraceDifference pivot = relevantDiffs.get(relevantDiffs.size()/2);
		int pivotSize = pivot.getExecutedActivities().size();
		
		List<TraceDifference> less = new ArrayList<TraceDifference>();
		List<TraceDifference> greater = new ArrayList<TraceDifference>();
		
		for (int i = 0; i < relevantDiffs.size(); i++) {
			if (i == relevantDiffs.size() / 2) {
				continue;
			}
			TraceDifference diff = relevantDiffs.get(i);
			int size = diff.getExecutedActivities().size();
			if (size <= pivotSize) {
				less.add(diff);
			} else {
				greater.add(diff);
			}
		}
		ordered.addAll(this.orderByExecutedTraceSize(less));
		ordered.add(pivot);
		ordered.addAll(this.orderByExecutedTraceSize(greater));
		return ordered;
	}


}
