package eu.fbk.soa.process.node;

public class EndNode extends ActivityNode {

	private static int endNodeIndex = 0; 
	
	public EndNode() {
		super("End" + endNodeIndex);
		endNodeIndex++;
	}
	
	public String toString() {
		return this.activity.toString(); 
	}
}
