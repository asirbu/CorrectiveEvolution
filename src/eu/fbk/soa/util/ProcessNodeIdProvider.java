package eu.fbk.soa.util;

import java.util.HashMap;
import java.util.Map;

import org.jgrapht.ext.VertexNameProvider;

import eu.fbk.soa.process.node.ActivityNode;
import eu.fbk.soa.process.node.ProcessNode;


public class ProcessNodeIdProvider implements VertexNameProvider<ProcessNode> {

	private int nextID = 2;
    private final Map<ProcessNode, Integer> idMap = new HashMap<ProcessNode, Integer>();
	
	@Override
	public String getVertexName(ProcessNode vertex) {
		Integer id = idMap.get(vertex);
		if (id == null) {
			if (vertex instanceof ActivityNode && !idMap.containsValue(new Integer(1))) {
				ActivityNode node = (ActivityNode) vertex;
				if (node.getActivity().getName().startsWith("Start")) {
					id = 1;
				}
			}	
			if (id == null) {
				id = nextID++;
			}
			idMap.put(vertex, id);
		}
		return id.toString();
	}

}
