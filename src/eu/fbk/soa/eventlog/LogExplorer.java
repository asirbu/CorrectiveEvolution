package eu.fbk.soa.eventlog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.deckfour.xes.extension.std.XConceptExtension;
import org.deckfour.xes.extension.std.XLifecycleExtension;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.info.impl.XLogInfoImpl;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

import eu.fbk.soa.process.Activity;
import eu.fbk.soa.process.ProcessModel;
import eu.fbk.soa.process.Trace;


public class LogExplorer {

	static Logger logger = Logger.getLogger(LogExplorer.class);
	
	private int distinctTraceNr = 0;
	
	private int totalNrTraces;
	
	private ProcessModel model;
	
	private XConceptExtension conceptExt;
	
	private XLifecycleExtension lifecycleExt;
	
	private Map<String, List<String>> traceTypes;
	
	private Set<String> divergingTraces;
	
	private Set<String> modelTraces;
	
	private Map<String, Integer> nrOfTracesByType;
	
	private Map<String, Boolean> canBeReplayed;
	
	
	public LogExplorer(ProcessModel model) {
		this.model = model;	
		this.modelTraces = new HashSet<String>();
		conceptExt = XConceptExtension.instance();
		lifecycleExt = XLifecycleExtension.instance();
	}
	
	private void init() {
		canBeReplayed = new HashMap<String, Boolean>();
		traceTypes = new HashMap<String, List<String>>();
		nrOfTracesByType = new HashMap<String, Integer>();
		divergingTraces = new HashSet<String>();
	}
	
	
	public void processEventLog(XLog log) {
		init();
		processTracesInOriginalModel();
		
		XLogInfo info = XLogInfoImpl.create(log);
		totalNrTraces = info.getNumberOfTraces();
		
		logger.info("Processing event log: found " + 
				info.getNumberOfTraces() + " traces and a total of " + info.getNumberOfEvents() + " events");
		
		for (XTrace trace : log) {
			processTrace(trace);			
		}
		logger.info("Identified " + traceTypes.size() + " distinct traces, " +
				"of which " + divergingTraces.size() + " cannot be replayed by model");
	}
	
	private void processTracesInOriginalModel() {
		for (Trace trace : model.getDistinctTraces()) {
			distinctTraceNr++;
			String newTraceID = "" + distinctTraceNr;
			
			List<String> actNames = new ArrayList<String>();
			for (Activity act : trace.getActivities()) {
				actNames.add(act.getName());
			}
			traceTypes.put(newTraceID, actNames);
			this.nrOfTracesByType.put(newTraceID, 0);
			this.modelTraces.add(newTraceID);
			
			logger.debug("Added original trace: " + actNames);
		}
	}
	
	private void processTrace(XTrace trace) {
//		System.out.println("Processing trace " + index);
		List<String> traceActivities = this.getActivities(trace);
		
		String traceID = this.getTraceType(traceActivities);
		if (traceID != "") {
			int nrOfTraces = this.nrOfTracesByType.get(traceID);
			nrOfTraces++;
			nrOfTracesByType.put(traceID, nrOfTraces);
			canBeReplayed.put(traceID, modelTraces.contains(traceID));
			
		} else {
			distinctTraceNr++;
			String newTraceID = "" + distinctTraceNr;
			traceTypes.put(newTraceID, traceActivities);
			this.nrOfTracesByType.put(newTraceID, 1);

			canBeReplayed.put(newTraceID, false);
			this.divergingTraces.add(newTraceID);
		}
	}
	
	
	private List<String> getActivities(XTrace trace) {
		List<String> traceActivities = new ArrayList<String>();
		
		for (XEvent event : trace) {
			String actName = "";
			actName += conceptExt.extractName(event);
			String trans = lifecycleExt.extractTransition(event);
			if (trans != null) {
				actName += " " + trans;
			}
			traceActivities.add(actName);
		}
		return traceActivities;
	}
	
	private String getTraceType(List<String> traceActivities) {
		List<String> noEndTrace1 = this.getNoEndTrace(traceActivities);
		
		for (String traceID : traceTypes.keySet()) {
			List<String> traceType = traceTypes.get(traceID);
			List<String> noEndTrace2 = this.getNoEndTrace(traceType);
			
			if (equals(noEndTrace1, noEndTrace2)) {
				if (this.modelTraces.contains(traceID)) {
					logger.debug("Found equal traces:\n\t " + traceActivities + "\n\t and " + traceType);
				}
				return traceID;
			} 
		}
		
		return "";
	}
	
	private boolean equals(List<String> trace1, List<String> trace2) {		
		
		if (trace1.size() != trace2.size()) {
			return false;
		}		
		for (int i = 0; i < trace1.size(); i++) {
			if (!trace1.get(i).equals(trace2.get(i))) {
				return false;
			}
		}
		return true;
	}
	
	private List<String> getNoEndTrace(List<String> trace) {
		if (trace.get(trace.size() - 1).startsWith("End")) {
			List<String> noEnd = new ArrayList<String>(trace);
			noEnd.remove(trace.size() - 1);
			return noEnd;
		}
		return trace;
	}



	
	
	public Set<String> getDivergingTraces() {
		return this.divergingTraces;
	}



	public int getTotalNrTraces() {
		return this.totalNrTraces;
	}



	public Map<String, Integer> getNrOfTracesByType() {
		return this.nrOfTracesByType;
	}



	public Map<String, List<String>> getTraceTypes() {
		return this.traceTypes;
	}
	
}
