package eu.fbk.soa.evolution.engine.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import eu.fbk.soa.evolution.Correction;
import eu.fbk.soa.evolution.sts.Action;
import eu.fbk.soa.evolution.sts.STS;
import eu.fbk.soa.process.Activity;
import eu.fbk.soa.process.Adaptation;
import eu.fbk.soa.process.EaGLeGoal;
import eu.fbk.soa.process.Goal;
import eu.fbk.soa.process.GoalWithPriorities;
import eu.fbk.soa.process.ProcessModel;
import eu.fbk.soa.process.StateFormula;
import eu.fbk.soa.process.Trace;
import eu.fbk.soa.process.domain.DomainObject;
import eu.fbk.soa.util.StringUtils;

public class ProblemToSMV {
	
	static Logger logger = Logger.getLogger(ProblemToSMV.class);
	
	private ProcessModel originalModel;
	
	private List<Correction> corrections;
	
//	private Set<ProcessModel> adaptationModels;
	
	private Map<ProcessModel, String> modelIDs;
	
	private Map<Action, Activity> actionTable;
	
	private Map<DomainObject, String> objIDs;
	
	private ProblemToSTS pb2STS;

	private STSToSMV sts2smv;

	private Map<Trace, String> traceIDs;

	private Map<String, String> id2header;

	private Map<Condition, String> properCondIDs; 
	
	private String semID = "semaphore";
	
	private Goal goal;
	
	public ProblemToSMV(ProcessModel model, List<Correction> corrections) {
		initialize(model, corrections);
	}
	
	public ProblemToSMV(ProcessModel model, Goal goal, List<Correction> corrections) {
		initialize(model, corrections);
		this.goal = goal;
	}
	
	private void initialize(ProcessModel model, List<Correction> corrections) {
		this.originalModel = model;
		this.corrections = corrections;
		
		actionTable = new HashMap<Action, Activity>();
		id2header = new HashMap<String, String>();
		
		generateIDs();
		pb2STS = new ProblemToSTS(model, corrections, properCondIDs.keySet());
	}
	
	private void generateIDs() {
		objIDs = new HashMap<DomainObject, String>();
		modelIDs = new HashMap<ProcessModel, String>();
		traceIDs = new HashMap<Trace, String>();
		new HashMap<StateFormula, String>();
		properCondIDs = new HashMap<Condition, String>();
		
		Set<DomainObject> objects = new HashSet<DomainObject>();
		objects.addAll(originalModel.getRelatedDomainObjects());
		for (Correction corr : corrections) {
			objects.addAll(corr.getRelatedDomainObjects());
		}
		for (DomainObject obj : objects) {
			objIDs.put(obj, "obj" + obj.getName());
		}
		
		modelIDs.put(originalModel, "originalModel");
		int index = 1;
		
		for (int i = 0; i < corrections.size(); i++) {
			Correction corr = corrections.get(i);
			ProcessModel adModel = corr.getAdaptation().getAdaptationModel();
			modelIDs.put(adModel, "ad" + adModel.getName());
			
			traceIDs.put(corr.getTrace(), "t" + index);
			
			if (!corr.getCondition().isEmpty()) {
				properCondIDs.put(new Condition(corr.getCondition(), index), "cond" + index);
			}
			index++;
		}
	}
	
	
	public String translateProblemToSMV() {
		logger.info("Translating problem to SMV");
		pb2STS.translateProblem2STS();
		actionTable = pb2STS.getActionTable();
		
		sts2smv = new STSToSMV(modelIDs, objIDs, properCondIDs, getIDToSTS());
		
		StringBuffer translation = new StringBuffer();
		translation.append(translateOriginalModel());
		
		int index = 1; 
		for (Correction corr : corrections) {
			translation.append(translateTrace(corr.getTrace(), index));
			translation.append(translateAdaptation(corr));
			translation.append(translateCondition(corr.getCondition(), index));
			index++;
		}
		
		translation.append(translateSemaphore());
		for (DomainObject object : objIDs.keySet()) {
			translation.append(translateDomainObject(object));
		}
		
		translation.append(this.createMainModule());
		return translation.toString();	
	}
	
	
	public Map<String, STS> getIDToSTS() {
		Map<String, STS> id2STS = new HashMap<String, STS>();
		for (ProcessModel model : this.modelIDs.keySet()) {
			id2STS.put(modelIDs.get(model), pb2STS.getProcessModelSTS(model));
		}
		for (Trace trace : this.traceIDs.keySet()) {
			id2STS.put(traceIDs.get(trace), pb2STS.getTraceSTS(trace));
		}
		
		for (Condition cond : this.properCondIDs.keySet()) {
			id2STS.put(properCondIDs.get(cond), pb2STS.getConditionSTS(cond));
		}	
		id2STS.put(semID, pb2STS.getSemaphoreSTS());
		
		return id2STS;
	}
	

	private String translateSemaphore() {
		String translation = "---------------------\nMODULE Semaphore";
		
		Set<String> params = new HashSet<String>(modelIDs.values());
		String paramStr = "(" + StringUtils.getCommaSeparatedString(params) + ")";	
		translation += paramStr + "\n\n";
		id2header.put(semID, "Semaphore" + paramStr);
		
		translation += sts2smv.translateSTS(this.pb2STS.getSemaphoreSTS());
		return translation;
	}

	
	private String translateCondition(StateFormula formula, int index) {
		Condition properCond = new Condition();
		for (Condition condition : this.properCondIDs.keySet()) {
			if (condition.getIndex() == index && 
					condition.getFormula().equals(formula)) {
				properCond = condition;
			}
		}
		if (properCond.isEmpty()) {
			return "";
		}
		
		STS condSTS = pb2STS.getConditionSTS(properCond);
		if (condSTS.getStates().size() < 2) {
			return "";
		}
		
		String translation = createConditionHeader(properCond) +
			sts2smv.translateSTS(condSTS, properCond);
		return translation;
	}
	
	private String createConditionHeader(Condition cond) {
		String header = "---------------------\nMODULE Condition" + cond.getIndex();
		
		Set<String> params = getParameters(properCondIDs.get(cond));
//		for (DomainObject obj : objIDs.keySet()) {
//			params.add(objIDs.get(obj));
//		}
//		params.addAll(modelIDs.values());
//		Trace trace = corrections.get(cond.getIndex()-1).getTrace();
//		params.add(traceIDs.get(trace));
//		params.add(semID);
		String paramStr = "(" + StringUtils.getCommaSeparatedString(params) + ")";	
		header += paramStr + "\n\n";
		
		id2header.put(properCondIDs.get(cond), "Condition"+ cond.getIndex() + paramStr);
		return header;
		
	}

	private Set<String> getParameters(String myID) {
		Set<String> params = new HashSet<String>();
		params.addAll(objIDs.values());
		params.addAll(modelIDs.values());
		params.add(semID);
		params.addAll(traceIDs.values());
		params.addAll(properCondIDs.values());
		params.remove(myID);
		return params;
	}
	
	private String translateTrace(Trace trace, int index) {
//		if (trace.isEmpty()) return "";
		
		STS traceSTS = pb2STS.getTraceSTS(trace);
		String translation = 
			createTraceHeader(trace, index) + sts2smv.translateSTS(traceSTS, trace);
		return translation;
	}

	private String createTraceHeader(Trace trace, int index) {
		String header = "---------------------\nMODULE Trace" + index;
		
		Set<String> params = new HashSet<String>();
		params.addAll(modelIDs.values());
		for (DomainObject obj : objIDs.keySet()) {
			params.add(objIDs.get(obj));
		}
//		for (int i = 0; i < index; i++) {
//			Correction corr = this.corrections.get(i);
//			ProcessModel prevAdModel = corr.getAdaptation().getAdaptationModel();
//			params.add(modelIDs.get(prevAdModel));
//		}
		
		String paramStr = "(" + StringUtils.getCommaSeparatedString(params) + ")";	
		header += paramStr + "\n\n";
		id2header.put(traceIDs.get(trace), "Trace"+ index + paramStr);
		return header;
	}
	
	private String translateAdaptation(Correction correction) {
		Adaptation adaptation = correction.getAdaptation();
		ProcessModel adModel = adaptation.getAdaptationModel();
		
		Set<String> params = this.getParameters(modelIDs.get(adModel));
//				new HashSet<String>(objIDs.values());
//		params.add(modelIDs.get(originalModel));
//		params.add(semID);
//		Trace trace = correction.getTrace();
//		params.add(traceIDs.get(trace));
				
		String translation = this.createModelHeader(adModel, params);		
		
		STS sts = pb2STS.getAdaptationSTS(adaptation);		
		translation += sts2smv.translateSTS(sts, adModel);
		return translation;
	}
	
	private String createModelHeader(ProcessModel model, Set<String> params) {
		String header = "---------------------\nMODULE " + model.getName();
		String paramStr = "(" + StringUtils.getCommaSeparatedString(params) + ")";	
		header += paramStr + "\n\n";
		this.id2header.put(modelIDs.get(model), model.getName() + paramStr);
//		modelHeaders.put(model, model.getName() + paramStr);
		return header;
	}

	private String translateOriginalModel() {	
		Set<String> params = new HashSet<String>(objIDs.values());
		params.add(semID);
		for (ProcessModel m : modelIDs.keySet()) {
			if (!m.equals(originalModel)) {
				params.add(modelIDs.get(m));
			}
		}
		String translation = createModelHeader(originalModel, params);
		translation += sts2smv.translateSTS(pb2STS.getMainSTS(), originalModel);
		return translation;
	}

	
	private String translateDomainObject(DomainObject object) {
		String translation = "---------------------\n" + "MODULE "
			+ object.getName() + " ";
		
		Set<String> ids = this.getParameters(objIDs.get(object));
//		new HashSet<String>();
//		ids.addAll(modelIDs.values());
//		ids.addAll(traceIDs.values());
//		ids.addAll(properCondIDs.values());
		
		String paramList = "(" + StringUtils.getCommaSeparatedString(ids) + ")";
		id2header.put(objIDs.get(object), object.getName() + paramList);
		translation += paramList + "\n\n";

		STS sts = pb2STS.getDomainObjectSTS(object);
		translation += sts2smv.translateSTS(sts, object);

		translation += "\n";
		return translation;
	}
	
	
	private String createMainModule() {
		String translation = "---------------------\nMODULE main\n\n";

		for (String id : id2header.keySet()) {
			translation += "VAR " + id + ": " + id2header.get(id) + ";\n";
		}	

		List<String> noactions = new ArrayList<String>();
		for (ProcessModel model : modelIDs.keySet()) {
			String modelID = this.modelIDs.get(model);
			
			String noInputAct = "no" + modelID + "Input";
			translation += "\nDEFINE " + noInputAct + " := (" + modelID + ".input = UNDEF);"; 
			noactions.add(noInputAct);
		
			String noOutputAct = "no" + modelID + "Output";
			STS modelSTS = pb2STS.getProcessModelSTS(model);
			if (!modelSTS.getOutputActions().isEmpty()) {
				translation += "\nDEFINE " + noOutputAct + " := (" + modelID + ".output = UNDEF);"; 
				noactions.add(noOutputAct);
			}
		}
		
		Map<String, Set<String>> exceptions = new HashMap<String, Set<String>>();
		Map<Condition, String> noCondActions = new HashMap<Condition, String>();
		for (Condition properCond : this.properCondIDs.keySet()) {
			STS condSTS = pb2STS.getConditionSTS(properCond);
			if (condSTS.getStates().size() < 2) {
				continue;
			}
			String condID = this.properCondIDs.get(properCond);
			translation += "\nDEFINE no" + condID + " := (" + condID + ".output = UNDEF);"; 
			noCondActions.put(properCond, "no" + condID);
			noactions.add("no" + condID);
		}
		
		for (Condition cond : this.properCondIDs.keySet()) {
			String noCond = noCondActions.get(cond);
			Set<String> samePoint = new HashSet<String>();
			for (Condition samePointCond : this.getSamePointConditions(cond)) {
				samePoint.add(noCondActions.get(samePointCond));
			}
			if (!samePoint.isEmpty()) {
				exceptions.put(noCond, samePoint);
			}
		}

		translation += createOneActionAtATimeRule(noactions, exceptions);
		
//		if (goal != null && !goal.isEmpty()) {
//			translation += "\nGOAL ONEOF(";
//			
//			String formula = sts2smv.translateStateFormula(goal.getFormula());
//			translation += "\n" + formula + ", 1;";
//			translation += "\n" + formula + ", 0;";
//			translation += "\n)";
//		}
		if (goal != null) {
			translation += translateRequirementsToSMV(goal); 
		}
		
		return translation;
	}
	
	
	private String createOneActionAtATimeRule(List<String> noactions, Map<String, Set<String>> exceptions) {
		String translation = "\nDEFINE only_one_action :=\n !(";
		for (int i = 0; i < noactions.size(); i++) {
			if (i > 0) {
				translation += " & ";
			}
			translation += noactions.get(i);
		}
		translation += ")";
		
		for (int i = 0; i < noactions.size(); i++) {
			String noaction1 = noactions.get(i);
			translation += "\n & ("+ noaction1 + " | (";
			
			Set<String> except = exceptions.get(noaction1);
			int index = 0;
			for (int j = 0; j < noactions.size(); j++) {
				if (j == i) {
					continue;
				}
				if (except == null || !except.contains(noactions.get(j))) {
					if (index > 0) {
						translation += " & ";
					}
					translation += noactions.get(j);
					index++;
				}
			}
			translation += "))";
		}
		translation += ";\n\nTRANS only_one_action";
		return translation;
	}
		
	
	private String translateRequirementsToSMV(Goal goal) {
		String goal2smv = "";
		
		if (goal instanceof GoalWithPriorities) {
			GoalWithPriorities priorities = (GoalWithPriorities) goal;
			
			if (!(priorities.getPremise().equalsTop())) {
				throw new UnsupportedOperationException(
						"Currently supporting goals over object states, with T (Top) as premise.\n"
								+ "Cannot process the goal: " + goal.toString());
			}
			int nrOfPreferences = priorities.getResult().size();
			//System.out.println("Prefs " + nrOfPreferences);
			for (int i = 0; i < nrOfPreferences; i++) {
				StateFormula formula = priorities.getResult().get(i);
				String formulaStatement = sts2smv.translateStateFormula(formula);
				
				int pref = 2 * (nrOfPreferences - i) - 1;
				goal2smv += formulaStatement + ", " + pref + ";\n";
				goal2smv += formulaStatement + ", " + (pref-1) + ";\n";
			}
		}
		
		if (goal instanceof EaGLeGoal) {
			EaGLeGoal eagleGoal = (EaGLeGoal) goal;
			String formula = sts2smv.translateStateFormula(eagleGoal.getFormula());
			goal2smv += "\n" + formula + ", 1;";
			goal2smv += "\n" + formula + ", 0;";
		}
		
		goal2smv = "\nGOAL ONEOF(\n" + goal2smv + ")\n";
		return goal2smv;
	}
	
	private Set<Condition> getSamePointConditions(Condition cond) {
		Set<Condition> samePointConds = new HashSet<Condition>();	
		Correction corr = corrections.get(cond.getIndex()-1);
		
		for (Condition otherCond : this.properCondIDs.keySet()) {
			if (otherCond.getIndex() != cond.getIndex()) {
				Correction otherCorr = corrections.get(otherCond.getIndex()-1);
				if (otherCorr.isAtSamePoint(corr)) {
					samePointConds.add(otherCond);
				}
			}
		}		
		return samePointConds;
	}
	

	public Map<Action, Activity> getActionActivityCorrespondences() {
		return actionTable;
	}

	public Map<Action, Condition> getActionConditionCorrespondences() {
		return pb2STS.getActionConditionCorrespondences();
	}	
}


	
	
