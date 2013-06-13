package eu.fbk.soa.evolution.sts.impl;

import eu.fbk.soa.evolution.sts.State;

public class DefaultState implements State {

	private int hashcode = 0;
	
	private String name;

	public DefaultState(String name) {
		this.name = name;
//		this.hashcode = name.hashCode();
	}

	public String toString() {
		return this.name;
	}
	
	@Override
	public String getName() {
		return this.name;
	}

	public int hashCode() {
		return hashcode;
	}
	
	public boolean equals(Object obj) {
		if (!(obj instanceof DefaultState)) {
			return false;
		}
		DefaultState state = (DefaultState) obj;
		return (name.equals(state.getName()));
	}

	
	@Override
	public void setName(String string) {
		this.name = string;
		
	}
}
