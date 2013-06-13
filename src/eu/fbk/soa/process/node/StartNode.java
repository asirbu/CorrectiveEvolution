package eu.fbk.soa.process.node;

import eu.fbk.soa.process.Activity;



public class StartNode extends ActivityNode {
	
	private static int startNodeIndex = 0; 
	
	public StartNode() {
		super("Start" + startNodeIndex);
		startNodeIndex++;
	}
	
	public StartNode(Activity activity) {
		super(activity);
	}
	
	public boolean equals(Object object) {
		if (object instanceof StartNode) {
			StartNode sn = (StartNode) object;
			return this.activity.equals(sn.getActivity());
		}
		return false;
	}
	
	public String toString() {
		return this.activity.toString(); 
	}
}
