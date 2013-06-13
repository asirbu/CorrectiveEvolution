package eu.fbk.soa.util;

import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.jgrapht.ext.ComponentAttributeProvider;
import org.jgrapht.ext.EdgeNameProvider;
import org.jgrapht.ext.VertexNameProvider;

import eu.fbk.soa.process.ProcessEdge;
import eu.fbk.soa.process.ProcessModel;
import eu.fbk.soa.process.node.ProcessNode;

public class OrderedNodesDOTExporter {

    private VertexNameProvider<ProcessNode> vertexIDProvider;
    private VertexNameProvider<ProcessNode> vertexLabelProvider;
    private EdgeNameProvider<ProcessEdge> edgeLabelProvider;
    private ComponentAttributeProvider<ProcessNode> vertexAttributeProvider;
//    private ComponentAttributeProvider<ProcessEdge> edgeAttributeProvider;

    private String indent = "  ";
    
    private String connector = " -> ";

    public OrderedNodesDOTExporter() {
        this.vertexLabelProvider = 
			new ProcessNodeNameProvider();
        this.edgeLabelProvider = new ConditionEdgeNameProvider();;
        
        vertexIDProvider = new ProcessNodeIdProvider();
		vertexAttributeProvider = new NodePropertiesProvider();
    }

    public void export(Writer writer, ProcessModel g) {
    	PrintWriter out = new PrintWriter(writer);
    	out.println("digraph G {");
    	out.println("edge[penwidth = 3];");
    	out.println("node[penwidth = 3];");

    	List<ProcessNode> nodes = new ArrayList<ProcessNode>();
    	for (ProcessNode v : g.getProcessNodes()) {
    		if (g.inDegreeOf(v) == 0) {
    			nodes.add(v);
    			this.collectOrderedChildren(v, g, nodes);
    		}
    	}

    	writeNodes(nodes, out);
    	writeEdges(g, out);
    	out.println("}");
    	out.flush();
    }

    private void collectOrderedChildren(ProcessNode node, 
    		ProcessModel g, List<ProcessNode> alreadyIn) {
    	
    	List<ProcessNode> toVisit = new ArrayList<ProcessNode>();
    	for (ProcessEdge edge : g.edgesOf(node)) {
    		ProcessNode child = g.getEdgeTarget(edge);
    		if (!alreadyIn.contains(child)) {
    			toVisit.add(child);
    			alreadyIn.add(child);
    		}
    	}
    	for (ProcessNode child : toVisit) {
    		this.collectOrderedChildren(child, g, alreadyIn);
    	}
    }
    
    private void writeNodes(List<ProcessNode> nodes, PrintWriter out) {
    	for (ProcessNode v : nodes) {
    		out.print(indent + getVertexID(v));

    		String labelName = null;
    		if (vertexLabelProvider != null) {
    			labelName = vertexLabelProvider.getVertexName(v);
    		}
    		Map<String, String> attributes = null;
    		if (vertexAttributeProvider != null) {
    			attributes = vertexAttributeProvider.getComponentAttributes(v);
    		}
    		renderAttributes(out, labelName, attributes);
    		out.println(";");
    	}
    }
    
    private void writeEdges(ProcessModel g, PrintWriter out) {
    	
       	for (ProcessEdge e : g.edgeSet()) {
    		String source = getVertexID(g.getEdgeSource(e));
    		String target = getVertexID(g.getEdgeTarget(e));

    		out.print(indent + source + connector + target);
    		String labelName = edgeLabelProvider.getEdgeName(e);
    		renderAttributes(out, labelName, null);

    		out.println(";");
       	}
    }

    private void renderAttributes(PrintWriter out,
        String labelName, Map<String, String> attributes) {
        if ((labelName == null) && (attributes == null)) {
            return;
        }
        out.print(" [ ");
        if ((labelName == null) && (attributes != null)) {
            labelName = attributes.get("label");
        }
        if (labelName != null) {
            out.print("label=\"" + labelName + "\" ");
        }
        if (attributes != null) {
            for (Map.Entry<String, String> entry : attributes.entrySet()) {
                String name = entry.getKey();
                if (name.equals("label")) {
                    // already handled by special case above
                    continue;
                }
                out.print(name + "=\"" + entry.getValue() + "\" ");
            }
        }
        out.print("]");
    }

    private String getVertexID(ProcessNode v) {
        // use the associated id provider for an ID of the given vertex
        String idCandidate = vertexIDProvider.getVertexName(v);

        // now test that this is a valid ID
        boolean isAlphaDig = idCandidate.matches("[a-zA-Z]+([\\w_]*)?");
        boolean isDoubleQuoted = idCandidate.matches("\".*\"");
        boolean isDotNumber =
            idCandidate.matches("[-]?([.][0-9]+|[0-9]+([.][0-9]*)?)");
        boolean isHTML = idCandidate.matches("<.*>");

        if (isAlphaDig || isDotNumber || isDoubleQuoted || isHTML) {
            return idCandidate;
        }

        throw new RuntimeException(
            "Generated id '" + idCandidate + "'for vertex '" + v
            + "' is not valid with respect to the .dot language");
    }

}
