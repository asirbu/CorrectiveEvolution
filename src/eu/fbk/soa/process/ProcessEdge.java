package eu.fbk.soa.process;

import org.jgrapht.graph.DefaultEdge;

public class ProcessEdge extends DefaultEdge {

	private static final long serialVersionUID = -5944643088183585456L;

	private StateFormula condition = StateFormula.getTop();
	
	public void setCondition(StateFormula cond) { 
		this.condition = cond;
	}

	public StateFormula getCondition() {
		return condition;
	}
	
	public String toString() {
		return "Edge from " + this.getSource() + " to " + this.getTarget() +
			" with condition " + this.condition;
	}
	
	
}
