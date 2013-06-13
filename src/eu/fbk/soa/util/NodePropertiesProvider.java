package eu.fbk.soa.util;

import java.util.HashMap;
import java.util.Map;

import org.jgrapht.ext.ComponentAttributeProvider;

import eu.fbk.soa.process.node.ActivityNode;
import eu.fbk.soa.process.node.ControlConnector;
import eu.fbk.soa.process.node.ProcessNode;


public class NodePropertiesProvider implements ComponentAttributeProvider<ProcessNode> {
	private Map<String, String> startProperties;
	
	private Map<String, String> actProperties;
	
	private Map<String, String> connProperties;
	
	public NodePropertiesProvider() {
		startProperties = new HashMap<String, String>();
		startProperties.put("shape", "oval");
		
		actProperties = new HashMap<String, String>();
		actProperties.put("shape", "box");
		
		connProperties = new HashMap<String, String>();
		connProperties.put("shape", "diamond");
	}
	
	@Override
	public Map<String, String> getComponentAttributes(ProcessNode component) {
//		if (component instanceof StartNode || component instanceof EndNode) {
//			return startProperties;
//		}
		if (component instanceof ActivityNode) {
			return actProperties;
		}
		if (component instanceof ControlConnector) {
			return connProperties;
		}
		return new HashMap<String, String>();
	}

}
