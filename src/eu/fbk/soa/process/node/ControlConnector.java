package eu.fbk.soa.process.node;


public abstract class ControlConnector extends ProcessNode {

	private static int connectorIndex = 0; 
	
	int connectorId;
	
	public ControlConnector() {
		this.connectorId = connectorIndex;
		connectorIndex++;
	}
	
}
