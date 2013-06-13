package eu.fbk.soa.evolution.sts.impl;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import eu.fbk.soa.evolution.sts.Action;



public class NusmvAction implements Action {

	static Logger logger = Logger.getLogger(NusmvAction.class);
	
	private static int nrActions = 1;
	
	private int index;
	
	private String name = "";
	
	private Map<String, String> variableValues;
	
	private boolean isInput = true;
	
	
	/**
	 * @throws IllegalArgumentException if all input/output actions are UNDEF
	 */
	public NusmvAction(Map<String, String> varValues) throws IllegalArgumentException {		
		index = nrActions;
		nrActions++;
		variableValues = new HashMap<String, String>();
				
		String actionBody = "";
		
		for (String key : varValues.keySet()) {
			if (key.contains("input") || key.contains("output")) {
				String value = varValues.get(key);
				
				if (!value.contains("UNDEF")) {
					variableValues.put(key, value);
					actionBody += key + " = " + value + "\n";
//					name = key + " = " + value;
					name = value;
					logger.trace("Created action: " + name);
					if (key.contains("output")) {
						this.isInput = false;
					}
				}
			}
		}
		if (name == "") {
			throw new IllegalArgumentException("No action defined in:\n " + actionBody);
		}
	}
	
	public NusmvAction() {		
		index = nrActions;
		nrActions++;
		variableValues = new HashMap<String, String>(); 
		
	}
	
	
	public String toString() {
		String stateStr = "Action " + index + "\n";
		for (String var : variableValues.keySet()) {
			stateStr += var + " = " + variableValues.get(var) + "\n";
		}
		return stateStr;
	}
	

	
	public boolean equals(Object obj) {
		if (!(obj instanceof NusmvAction)) {
			return false;
		}
		NusmvAction act = (NusmvAction) obj;
		boolean matches = true;
		
		for (String key : this.variableValues.keySet()) {
			String thisValue = variableValues.get(key);
			String actValue = act.getValue(key);
			if (actValue == null || !actValue.equals(thisValue)) {
				matches = false;
				break;
			}
		}
		
		return matches;
	}
	
	protected String getValue(String key) {
		return variableValues.get(key);
	}
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean isInputAction() {
		return isInput;
	}

	@Override
	public Object getRelatedEntity() {
		// TODO Auto-generated method stub
		return null;
	}
	
	public int hashCode() {
		return this.index + this.getName().hashCode();
	}

	@Override
	public boolean isRelatedToAnActivity() {
		return false;
	}
	
}
