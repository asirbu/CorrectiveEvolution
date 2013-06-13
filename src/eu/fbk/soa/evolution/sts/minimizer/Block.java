package eu.fbk.soa.evolution.sts.minimizer;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import eu.fbk.soa.evolution.sts.State;

public class Block {

	private Set<State> states;
	
//	private State main;
	
	public Block() {
		this.states = new HashSet<State>();
	}
	
	public Block(State state) {
		this();
		states.add(state);
	}
	
	public Block(Collection<State> inputStates) {
		this();
		this.states.addAll(inputStates);
	}

	public Set<State> getStates() {
		return states;
	}

//	public void setRepresentative(State state) {
//		this.main = state;
//	}
//
//	public State getRepresentative() {
//		return this.main;
//	}

	public int size() {
		return this.states.size();
	}

	public boolean containedIn(Block block) {
		boolean contained = true;
		for (State state : this.states) {
			if (!block.contains(state)) {
				contained = false;
				break;
			}
		}
		return contained;
	}

	public boolean contains(State state) {
		return states.contains(state);
	}
	
	public String toString() {
		String str = "Block: ";
		for (State s : states) {
			str += s.getName() + ", ";
		}
		return str;
	}
	
	public boolean equals(Object obj) {
		
		if (!(obj instanceof Block)) {
			return false;
		}
		
		Block block = (Block) obj;
		for (State s : this.states) {
			if (!block.contains(s)) {
				return false;
			}
		}
		for (State s : block.getStates()) {
			if (!this.states.contains(s)) {
				return false;
			}
		}
		return true;
		
	}
	
	public int hashCode() {
		int hashcode = 0;
		for (State s : states) {
			hashcode += s.hashCode();
		}
		return hashcode;
	}

	public boolean contains(Block block) {
		return this.states.containsAll(block.getStates());
	}
	
	public boolean contains(Collection<State> stateBag) {
		return this.states.containsAll(stateBag);
	}
}
