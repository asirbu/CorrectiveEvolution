package eu.fbk.soa.evolution.sts.nusmv;


import java.util.ArrayList;
import java.util.List;

import eu.fbk.soa.evolution.sts.State;
import eu.fbk.soa.evolution.sts.impl.NusmvState;

class StateTree {
	
	private Node root;
	
	private List<Node> totalNodes;
	
	StateTree() {
		totalNodes = new ArrayList<Node>();
		root = new Node(null);
		root.setParent(root);
		root.visit();
	}
	
	Node getRoot() {
		return this.root;
	}
	
	Node addNode(Node parent, NusmvState state) {
		Node newNode = new Node(parent, state);
		totalNodes.add(newNode);
		return newNode;
	}

	Node addNode(Node parent) {
		Node newNode = new Node(parent);
		totalNodes.add(newNode);
		return newNode;
	}
	
	boolean isVisited() {
		if (this.root.isLeaf()) {
			return false;
		}
		return root.subtreeIsVisited();
	}

}

class Node {

	private static int nrOfNodes = 0;

	private Integer id;

	private Node parent;

	private List<Node> children;

	private State state;

	private boolean visited = false;

	Node(Node parent) {
		this.id = nrOfNodes;
		this.parent = parent;
		children =  new ArrayList<Node>();
		nrOfNodes++;
	}

	boolean isLeaf() {
		return this.children.isEmpty();
	}

	Node(Node parent, State state) {
		this.id = nrOfNodes;
		this.parent = parent;
		this.children = new ArrayList<Node>();
		this.state = state;
		nrOfNodes++;
	}


	void setParent(Node newParent) {
		this.parent = newParent;
	}

	void visit() {
		this.visited = true;
	}

	boolean subtreeIsVisited() {
		if (!this.visited) {
			return false;
		}
		for (Node child : children) {
			if (! child.subtreeIsVisited()) {
				return false;
			}
		}
		return true;
	}

	List<Node> addChildren(List<? extends State> states) {
		List<Node> newNodes = new ArrayList<Node>();
		for (State state : states) {
			Node node = addChild(state);
			newNodes.add(node);
		}
		return newNodes;
	}

	Node addChild(State state) {
		for (Node child : children) {
			if (child.pointsTo(state)) {
				return child;
			}
		}
		Node newChild = new Node(this, state);
		children.add(newChild);
		return newChild;
	}

	boolean pointsTo(State s) {
		return (state.equals(s));
	}

	State getState() {
		return state;
	}

	List<Node> getChildren() {
		return this.children;
	}
}
