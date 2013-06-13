package eu.fbk.soa.evolution.sts.impl;

import java.util.HashMap;
import java.util.Map;

import eu.fbk.soa.evolution.sts.State;



public class NusmvState implements State {
	
	private static int nrStates = 1;
	
	private int index;
	
	private Map<String, String> variableValues;
	
	private String name;
	
	public NusmvState() {
		index = nrStates;
		name = "s"+index;
		nrStates++;
		variableValues = new HashMap<String, String>(); 
	}
	
	public NusmvState(Map<String, String> varValues) {		
		this();
		for (String key : varValues.keySet()) {
			if (key.contains(".state") || key.contains(".State")) {
				String value = varValues.get(key);
				variableValues.put(key, value);
			}
		}
	}
	
	public int hashCode() {
		return name.hashCode();
	}
	
	public void addVariableValue(String var, String value) {
		variableValues.put(var, value);
	}
	
	public String toString() {
		String stateStr = "State " + index + "\n";
		for (String var : variableValues.keySet()) {
			stateStr += var + " = " + variableValues.get(var) + "\n";
		}
		return stateStr;
	}

	public void addVariableValues(Map<String, String> varValues) {
		this.variableValues.putAll(varValues);
	}

	public String getValue(String key) {
		return variableValues.get(key);
	}

	public int getIndex() {
		return this.index;
	}

	
	public boolean equals(Object obj) {
		if (!(obj instanceof NusmvState)) {
			return false;
		}
		NusmvState state = (NusmvState) obj;
		return matches(variableValues, state.getVariableValues()) &&
			matches(state.getVariableValues(), variableValues);
	}
	
	private boolean matches(Map<String, String> varValues1, 
			Map<String, String> varValues2) {
	
		boolean matches = true;
		for (String var1 : varValues1.keySet()) {
			String value1 = varValues1.get(var1);
			String value2 = varValues2.get(var1);
			if (value2 == null || !value2.equals(value1)) {
				matches = false;
				break;
			}
		}
		return matches;
	}

	public Map<String, String> getVariableValues() {
		return this.variableValues;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String string) {
		name = string;
	}
	
	
}
