package eu.fbk.soa.process;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import eu.fbk.soa.process.node.EndNode;
import eu.fbk.soa.process.node.ProcessNode;
import eu.fbk.soa.process.node.StartNode;


public class Trace {

	private static Logger logger = Logger.getLogger(Trace.class);
	
	private List<Activity> activities = new ArrayList<Activity>();
	
	public Trace() {}
	
	public Trace(List<Activity> acts) {	
		activities.addAll(acts);
	}
	
	public Trace(Activity...acts) {
		for (Activity act : acts) {
			activities.add(act);
		}
	}

	public boolean isValidOnProcessModel(ProcessModel model) {
		if (!activities.isEmpty()) {
			if (!activityAppearsAsStartNode(activities.get(0), model)) {
				//First activity must correspond to a Start node
				System.out.println("First activity is not a start node");
				return false;
			}
			for (Activity act : activities) {
				if (! act.appearsAsNode(model)) {
					// Every activity must correspond to a node in the process model
					return false;
				}
			}
		}
		return true;
	}
	
	private boolean activityAppearsAsStartNode(Activity act, ProcessModel model) {
		for (ProcessNode node : model.getProcessNodes(act)) {
			if (node instanceof StartNode) {
				return true;
			}
		}
		return false;
	}
	
	private static boolean activityAppearsAsEndNode(Activity act, ProcessModel model) {
		for (ProcessNode node : model.getProcessNodes(act)) {
			if (node instanceof EndNode) {
				return true;
			}
		}
		return false;
	}
	
	
	
	public boolean isComplete(ProcessModel model) {
		if (!activities.isEmpty()) {		
			if (activityAppearsAsEndNode(getLastActivity(), model)) {
				return true;
			}
		}
		return false;
	}

	public boolean isEmpty() {
		return this.activities.isEmpty();
	}
	
	public int size() {
		return this.activities.size();
	}

	public Activity getLastActivity() {
		return activities.get(activities.size()-1);
	}
	
	public List<Activity> getActivities() {
		return activities;
	}
	
	public void addActivity(Activity act) {
		activities.add(act);
	}

	public void addAllActivities(List<Activity> actList) {
		this.activities.addAll(actList);
	}
	
	public String toString() {
		String str = "{";
		
		for (int i = 0; i < activities.size(); i++) {
			Activity act = activities.get(i);
			str += act.getName();
			if (i < activities.size()-1) {
				str += ", ";
			}
		}
		str += "}";
		
		return str;
	}
	
	public boolean equals (Object obj) {
		if (!(obj instanceof Trace)) {
			return false;
		}
		
		Trace trace = (Trace) obj;
		if (this.activities.size() != trace.size()) {
			return false;
		}
		for (int i = 0; i < trace.size(); i++) {
			if (!this.getActivity(i).equals(trace.getActivity(i))) {
				return false;
			}
		}
		
		return true;
	}

	public Activity getActivity(int i) {
		return this.activities.get(i);
	}
}
