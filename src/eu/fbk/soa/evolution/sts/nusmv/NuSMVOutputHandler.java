package eu.fbk.soa.evolution.sts.nusmv;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import eu.fbk.soa.evolution.sts.Action;
import eu.fbk.soa.evolution.sts.State;
import eu.fbk.soa.evolution.sts.impl.NusmvSTS;
import eu.fbk.soa.evolution.sts.impl.NusmvState;
import eu.fbk.soa.evolution.sts.impl.NusmvTransition;


class NuSMVOutputHandler {

	static List<State> addStatesToSTS(NusmvSTS sts, String consolePrint, String separator) {
		List<State> states = new ArrayList<State>();
		String[] tokens = consolePrint.split(separator);
		
		Map<String, String> firstVarValues = new HashMap<String, String>();
		for (String token : tokens) {
			if (token.startsWith(" State")) {
				Map<String, String> newVarValues = getVariableValues(token);
				if (firstVarValues.isEmpty()) {
					firstVarValues.putAll(newVarValues); 
				}
				Map<String, String> varValues = new HashMap<String, String>();
				varValues.putAll(firstVarValues);
				varValues.putAll(newVarValues);
				
				State state = sts.addState(varValues);
				states.add(state);
			}
		}
		return states;
	}
	
	
	
	static Map<String, String> getVariableValues(String str) {
		Map<String, String> varValues = new HashMap<String, String>();
		
		String[] tokens = str.split("\n");
		int i;
		for (i = 0; i < tokens.length; i++) {
			String token = tokens[i];
			//System.out.println(token);
			String[] pair = token.split(" = ");
			if (pair.length == 2) {
				String name = pair[0];
				name = name.trim();
				String value = pair[1];
				value = value.trim();
				varValues.put(name, value);
			}
		}
		return varValues;
	}
	
	
	static List<State> addInitialStatesToSTS(String fileContents, NusmvSTS sts) {
		String[] content = fileContents.split("AVAILABLE STATES");
		String lastToken = content[content.length -1];
		
		String[] stateList = lastToken.split("================= State =================");
		List<State> states = new ArrayList<State>();
		
		Map<String, String> initialVarValues = new HashMap<String, String>(); 
		
		for (String stateText : stateList) {
			String[] stateTokens = stateText.split("-------------------------");
			if (stateTokens.length <= 1) {
				continue;
			}
			String stateStr = stateTokens[1];
			Map<String, String> varValues = new HashMap<String, String>(initialVarValues);
			varValues.putAll(NuSMVOutputHandler.getVariableValues(stateStr));
			
			if (initialVarValues.isEmpty()) {
				initialVarValues.putAll(varValues);
			}
			State state = sts.addState(varValues);
			states.add(state);
		}
		return states;
	}
	
	
	static List<NusmvState> addTransitionsFromState(String newFileContents, 
			NusmvState parentState, NusmvSTS sts) {
		String[] runs = newFileContents.split("AVAILABLE STATES");
		String lastRun = runs[runs.length -1];

		String[] transitions = lastRun.split("================= State =================");
		
		List<NusmvState> states = new ArrayList<NusmvState>();
		
		int i = 0;
		List<Map<String, String>> varValuesList = new ArrayList<Map<String, String>>();
		
		for (String next : transitions) {
			String[] statements = next.split("This state is reachable through:");
			if (statements.length <= 1) {
				continue;
			}
			String stateStr = statements[0];
			String actionStr = statements[1];
			
			Map<String, String> varValues = new HashMap<String, String>(
					parentState.getVariableValues());
			if (i > 0) {
				Map<String, String> previous = varValuesList.get(i-1);
				varValues.putAll(previous);			
			}		
			varValues.putAll(NuSMVOutputHandler.getVariableValues(stateStr));
			
			varValuesList.add(varValues);
			NusmvState state = sts.addState(varValues);
			states.add(state);
			
			i++;
			
			if (actionStr.contains("Trace Description: Simulation Trace")) {
				String[] tokens = actionStr.split("Trace Description: Simulation Trace");
				actionStr = tokens[0];
			}
			Action action = sts.addAction(NuSMVOutputHandler.getVariableValues(actionStr));
			
			NusmvTransition t = new NusmvTransition(parentState, action, state);
			sts.addTransition(t);
		}
		return states;
	}

	
}
