package eu.fbk.soa.util;

import org.jgrapht.ext.VertexNameProvider;

import eu.fbk.soa.process.node.AndJoin;
import eu.fbk.soa.process.node.AndSplit;
import eu.fbk.soa.process.node.ProcessNode;
import eu.fbk.soa.process.node.XorJoin;
import eu.fbk.soa.process.node.XorSplit;


public class ProcessNodeNameProvider implements VertexNameProvider<ProcessNode> {

	@Override
	public String getVertexName(ProcessNode vertex) {
		String name = vertex.toString();
		
		if (vertex instanceof XorSplit) {
			name = "XorSplit";
		}
		if (vertex instanceof XorJoin) {
			name = "XorJoin";
		}
		if (vertex instanceof AndSplit) {
			name = "AndSplit";
		}
		if (vertex instanceof AndJoin) {
			name = "AndJoin";
		}
		return name;
	}

}
