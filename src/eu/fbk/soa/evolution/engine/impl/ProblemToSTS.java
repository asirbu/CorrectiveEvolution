package eu.fbk.soa.evolution.engine.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import eu.fbk.soa.evolution.Correction;
import eu.fbk.soa.evolution.sts.Action;
import eu.fbk.soa.evolution.sts.Clause;
import eu.fbk.soa.evolution.sts.Literal;
import eu.fbk.soa.evolution.sts.STS;
import eu.fbk.soa.evolution.sts.State;
import eu.fbk.soa.evolution.sts.Transition;
import eu.fbk.soa.evolution.sts.impl.DefaultAction;
import eu.fbk.soa.evolution.sts.impl.DefaultClause;
import eu.fbk.soa.evolution.sts.impl.DefaultLiteral;
import eu.fbk.soa.evolution.sts.impl.DefaultSTS;
import eu.fbk.soa.evolution.sts.impl.DefaultState;
import eu.fbk.soa.evolution.sts.impl.DefaultTransition;
import eu.fbk.soa.evolution.sts.impl.WildcardAction;
import eu.fbk.soa.process.Activity;
import eu.fbk.soa.process.Adaptation;
import eu.fbk.soa.process.Effect;
import eu.fbk.soa.process.ProcessModel;
import eu.fbk.soa.process.StateFormula;
import eu.fbk.soa.process.StateLiteralClause;
import eu.fbk.soa.process.Trace;
import eu.fbk.soa.process.domain.DomainObject;
import eu.fbk.soa.process.domain.EventLiteral;
import eu.fbk.soa.process.domain.ObjectEvent;
import eu.fbk.soa.process.domain.ObjectState;
import eu.fbk.soa.process.domain.ObjectTransition;
import eu.fbk.soa.process.domain.StateLiteral;
import eu.fbk.soa.process.node.ActivityNode;
import eu.fbk.soa.process.node.ProcessNode;

public class ProblemToSTS {

	private static Logger logger = Logger.getLogger(ProblemToSTS.class);
	
	private ProcessModel originalModel;
	
	private List<Correction> corrections;
	
	private STS mainSTS;
	
	private STS semaphore;
	
	private Map<Adaptation, STS> adaptationSTSs;
	
	private Map<Adaptation, Integer> adaptationIndexes;
	
	private Map<Trace, STS> traceSTSs;
	
	private Map<Condition, STS> conditionSTSs;
	
	private Map<DomainObject, STS> objectSTSs;
	
	private Set<ProcessModel> processModels;
	
	private int stateIndex;
	
	private Set<State> states;
	
	private Set<Action> inputActions;
	
	private Set<Action> outputActions;
	
	private Set<Transition> transitions;
	
	private Map<Action, Condition> action2cond;
	
	private Set<DomainObject> domainObjects;
	
	private Set<Condition> conditions;
	
	private State cState;
	
	private ProcessModelToSTS model2sts;
	
	public ProblemToSTS(ProcessModel model, List<Correction> corrections, Set<Condition> conds) {
		initializeSTSs();
		this.originalModel = model;
		this.corrections = corrections;
		
		processModels.add(originalModel);
		domainObjects.addAll(originalModel.getRelatedDomainObjects());
		
		for (Correction corr : corrections) {
			processModels.add(corr.getAdaptation().getAdaptationModel());
			domainObjects.addAll(corr.getRelatedDomainObjects());
		}
		this.conditions = conds;
	}
	
	private void initializeSTSs() {
		traceSTSs = new HashMap<Trace, STS>();
		adaptationSTSs = new HashMap<Adaptation, STS>();
		adaptationIndexes = new HashMap<Adaptation, Integer>();
		conditionSTSs = new HashMap<Condition, STS>();
		objectSTSs = new HashMap<DomainObject, STS>();
		action2cond = new HashMap<Action, Condition>();

		processModels = new HashSet<ProcessModel>();
		domainObjects = new HashSet<DomainObject>();
		conditions = new HashSet<Condition>();
		model2sts = new ProcessModelToSTS();
	}
	

	public void translateProblem2STS() {
		mainSTS = model2sts.transformProcessModel(originalModel);
		
		// TODO traces can also go on adaptations!
		int index = 1;
		List<ProcessModel> prevModels = new ArrayList<ProcessModel>();
		prevModels.add(originalModel);
		
		for (Correction corr : corrections) {
			Trace trace = corr.getTrace();
			STS tSTS = this.transformTrace(trace, prevModels,  index);
			traceSTSs.put(trace, tSTS);
			
			Adaptation adaptation = corr.getAdaptation();
			STS adSTS = this.transformAdaptationModel(corr, index);
			adaptationSTSs.put(adaptation, adSTS);
			adaptationIndexes.put(adaptation, index);
			prevModels.add(adaptation.getAdaptationModel());	
			index++;
		}
		
		for (DomainObject obj : this.domainObjects) {
			objectSTSs.put(obj, transformDomainObject(obj));
		}
		
		index = 1;
		for (Condition cond : conditions) {
			int prevIndex = this.getIndexOfPreviousAdaptation(cond.getIndex());

			STS condSTS = this.transformCondition(cond, prevIndex);
			conditionSTSs.put(cond, condSTS);
			index++;
		}
		semaphore = this.createSemaphore();
	}
	
	
	private int getIndexOfPreviousAdaptation(int index) {
		Adaptation adaptation = null;
		for (Adaptation ad : this.adaptationIndexes.keySet()) {
			if (adaptationIndexes.get(ad) == index) {
				adaptation = ad;
				break;
			}
		}
		ActivityNode fromNode = adaptation.getFromNode();		
		Transition fromTrans = model2sts.getCorrespondingTransition(fromNode);

		ProcessModel fromModel = (ProcessModel) fromTrans.getAction().getRelatedEntity();
		for (Adaptation ad : this.adaptationIndexes.keySet()) {
			if (ad.getAdaptationModel().equals(fromModel)) {
				return adaptationIndexes.get(ad);
			}
		}
		return 0;
	}
	
	
	public STS getSemaphoreSTS() {
		return semaphore;
	}

	private STS createSemaphore() {
		init();
		State initState = this.createState("s0");
		List<State> stateList = new ArrayList<State>();
		stateList.add(initState);
			
		int index = 1;
		for (Correction corr : corrections) {
			State s = createState("s" + index);
			stateList.add(s);
			index++;
		}
			
		STS sem = new DefaultSTS(states, initState, inputActions, 
				outputActions, transitions);
		sem.labelState(initState, "flag0");
		for (Transition trans : this.mainSTS.getTransitions()) {
			if (!trans.getAction().getName().contains("Resume")) {
				trans.addGuardClause(new DefaultClause(new DefaultLiteral("flag0")));
			}
		}
		
		index = 1;
		for (Correction corr : corrections) {
			sem.labelState(stateList.get(index), "flag" + index);
			Adaptation adaptation = corr.getAdaptation();
			STS adSTS = this.adaptationSTSs.get(adaptation);

			State initial = adSTS.getInitialState();
			int prevIndex = this.getIndexOfPreviousAdaptation(index);
			for (Transition initTrans : adSTS.getTransitionsFromState(initial)) {
				for (int i = prevIndex; i <= index; i++) {
					Action act = initTrans.getAction();
					if (!act.getName().contains("Resume")) {
						sem.addAction(act);
						Transition newTrans = new DefaultTransition(stateList.get(i), 
								new StateFormula(), act, stateList.get(index));
						sem.addTransition(newTrans);      
					}
				}
			}
			Action action = adSTS.getAction("Resume"+index);
			sem.addAction(action);
//			Transition resTrans1 = new DefaultTransition(initState,
//					new StateFormula(), action, initState);
//			sem.addTransition(resTrans1);
			Transition resTrans = new DefaultTransition(stateList.get(index),
					new StateFormula(), action, initState);
			sem.addTransition(resTrans);
			index++;
		}
		return sem;
	}

	private STS transformDomainObject(DomainObject obj) {
		init();
		State initialState = null;
		for (ObjectState state : obj.getStates()) {
			State newState = createState(state.getName());
			State initial = obj.getInitialState();
			if (initial != null && initial.equals(state)) {
				initialState = newState;
			}
		}
		
		Map<ObjectEvent, Map<Action, Activity>> event2action = 
						getRelatedActions(obj);

		for (ObjectTransition trans : obj.getTransitions()) {
			ObjectEvent event = trans.getObjectEvent();
			Map<Action, Activity> entries = event2action.get(event);
			if (entries == null || entries.isEmpty()) {
				continue;
			}
			
			for (Action action : entries.keySet()) {
				State startState = null;
				State endState = null;
				
				for (State state : states) {
					if (state.getName().equals(trans.getStartState().getName())) {
						startState = state;
					}
					if (state.getName().equals(trans.getEndState().getName())) {
						endState = state;
					}
				}
				if (startState != null && endState != null) { 
					createTransition(startState, new StateFormula(), 
						action, endState);
				}
			}
		}		
		return new DefaultSTS(states, initialState, inputActions, 
				outputActions, transitions);
	}
	
	
	private Map<ObjectEvent, Map<Action, Activity>> getRelatedActions(DomainObject object) {
		
		logger.trace("Collecting actions related to domain object " + object.getName());
		Map<ObjectEvent, Map<Action, Activity>> relatedActions =
			new HashMap<ObjectEvent, Map<Action, Activity>>();
		
		for (ObjectEvent event : object.getEvents()) {
			
			Map<Action, Activity> eventActions = new HashMap<Action, Activity>();
			EventLiteral evLit = new EventLiteral(object, event);
			
			Map<Action, Activity> actionTable = model2sts.getActionTable();
			
			for (Action action : actionTable.keySet()) {
				Activity activity = actionTable.get(action);
				Effect effect = activity.getEffect();
				if (effect.contains(evLit)) {
					eventActions.put(action, activity);
				} 
			}
			relatedActions.put(event, eventActions);
		}
		return relatedActions;
	}

	
	private STS transformCondition(Condition condition, int prevIndex) {
		init();
		State initialState = createState();
		logger.trace("Transforming condition " + condition);
		
		StateFormula condNoNegations = 
				condition.getFormula().getEquivalentFormulaWithoutNegations();			
		
		Map<ObjectTransition, DomainObject> transToUpdate = 
			new HashMap<ObjectTransition, DomainObject>();
		StateFormula condNoUncontrollable = 
			this.transformUncontrollableLiterals(condNoNegations, transToUpdate);
		
		if (!transToUpdate.isEmpty()) {
			State nextState = createState();
			int index = condition.getIndex();
			
			Action trigger = new DefaultAction("Trigger" + index, false, condition);
			Action notrigger = new DefaultAction("NoTrigger" + index, false, condition);
			
			action2cond.put(trigger, condition);
			action2cond.put(notrigger, condition.getNegation());
			
			outputActions.add(trigger);
			outputActions.add(notrigger);
			
			DefaultLiteral traceLit = new DefaultLiteral("Trace" + index);
			DefaultLiteral pointLit = new DefaultLiteral("Point" + index);
			DefaultClause flagClause = new DefaultClause();
			DefaultClause completeFlagClause = new DefaultClause();
			
			for (int i = 0; i < index; i++) {
				DefaultLiteral flagLit = new DefaultLiteral("flag" + i);
				completeFlagClause.addLiteral(flagLit);
				if (i >= prevIndex) {
					flagClause.addLiteral(flagLit);
				}
			}
			Set<Clause<? extends Literal>> clauses = 
				this.getRightPointClauses(traceLit, pointLit, flagClause);
			
			Transition triggerTrans = createTransition(initialState, 
					condNoUncontrollable, trigger, nextState);
			triggerTrans.addGuardClauses(clauses);
			
			Transition noTriggerTrans = createTransition(initialState, 
					new StateFormula(), notrigger, nextState);
			noTriggerTrans.addGuardClauses(clauses);
			
			Action reset = new DefaultAction("UNDEF", false, condition);
			outputActions.add(reset);
//			Transition resetTrans = createTransition(nextState, new StateFormula(), reset, initialState);
//			resetTrans.addGuardClause(flagClause);
			
			Transition stayTrans = createTransition(nextState, new StateFormula(), reset, nextState);
			for (int i = 0; i < index; i++) {
				DefaultLiteral negFlagLit = new DefaultLiteral("flag" + i, true);
				DefaultClause negFlagClause = new DefaultClause(negFlagLit);
				stayTrans.addGuardClause(negFlagClause);
			}
			
			Correction corr = corrections.get(condition.getIndex() - 1);
			ProcessModel matchingAdaptationModel = corr.getAdaptation().getAdaptationModel();
			
//			Action adUndef = new DefaultAction("UNDEF", true, matchingAdaptationModel);	
			
			for (ProcessModel model : processModels) {
				if (!model.equals(matchingAdaptationModel)) {
					Action otherwiseAct = new WildcardAction(true, model);
					inputActions.add(otherwiseAct);
					Transition trans = createTransition(nextState, StateFormula.getTop(), otherwiseAct, initialState);
					trans.addGuardClause(completeFlagClause);
				}
			}
			
			Transition resetTrans2 = createTransition(initialState, new StateFormula(), reset, initialState);
			for (int i = prevIndex; i < index; i++) {
				DefaultLiteral flagLit = new DefaultLiteral("flag" + i);
				DefaultClause guardClause = 
					new DefaultClause(traceLit.getNegation(), pointLit.getNegation(), flagLit.getNegation());
				resetTrans2.addGuardClause(guardClause);
			}
			
			this.updateDomainObjectTransitions(transToUpdate, trigger, clauses);
		}
		
		return new DefaultSTS(states, initialState, inputActions, 
				outputActions, transitions);		
	}
	
	private void updateDomainObjectTransitions(
			Map<ObjectTransition, DomainObject> transToUpdate, Action trigger, 
			Set<Clause<? extends Literal>> rightPointClauses) {
		
		for (ObjectTransition trans : transToUpdate.keySet()) {
			STS objSTS = objectSTSs.get(transToUpdate.get(trans));
			State startSt = objSTS.getState(trans.getStartState().getName());
			State endSt = objSTS.getState(trans.getEndState().getName());
			
			Transition newTrans = new DefaultTransition(startSt, 
					new StateFormula(), trigger, endSt);
			newTrans.addGuardClauses(rightPointClauses);
			objSTS.addTransition(newTrans);
		}
	}
	
	private Set<Clause<? extends Literal>> getRightPointClauses(
			DefaultLiteral traceLit, DefaultLiteral pointLit, DefaultClause flagClause) {
		
		DefaultClause traceClause = new DefaultClause(traceLit);
		DefaultClause pointClause = new DefaultClause(pointLit);
		Set<Clause<? extends Literal>> clauses = new HashSet<Clause<? extends Literal>>();
		clauses.add(traceClause);
		clauses.add(pointClause);
		clauses.add(flagClause);
		return clauses;
	}
	

	private StateFormula transformUncontrollableLiterals(StateFormula formula, 
			Map<ObjectTransition, DomainObject> transToUpdate) {
		StateFormula condNoUncontrollable = new StateFormula();
				
		for (StateLiteralClause clause : formula.getClauses()) {
			StateLiteralClause newClause = new StateLiteralClause();
			for (StateLiteral lit : clause.getLiterals()) {
				newClause.addLiteral(lit);
				
				DomainObject obj = lit.getDomainObject();
				for (ObjectTransition trans : obj.getTransitionsToState(lit.getState())) {
					
					if (!trans.getObjectEvent().isControllable()) {
						logger.debug("Found uncontrollable event " + trans.getObjectEvent());
						transToUpdate.put(trans, obj);
						newClause.addLiteral(new StateLiteral(obj, trans.getStartState()));
					}
				}
			}
			condNoUncontrollable.addClause(newClause);
		}
		return condNoUncontrollable;
	}
	
	
	public STS getMainSTS() {
		return mainSTS;
	}
	
	public STS getTraceSTS(Trace trace) {
		return this.traceSTSs.get(trace);
	}
	
	public STS getAdaptationSTS(Adaptation adaptation) {
		return this.adaptationSTSs.get(adaptation);
	}
	
	private void init() {
		this.stateIndex = 0;
		this.states = new HashSet<State>();
		
		inputActions = new HashSet<Action>();
		outputActions = new HashSet<Action>();
		transitions = new HashSet<Transition>();
	}
	
	
	private State createState() {
		return createState("s" + stateIndex);
	}
	
	private State createState(String name) {
		State newState = new DefaultState(name);
		stateIndex++;
		states.add(newState);
		return newState;
	}
	
	private Transition createTransition(State beginState, StateFormula cond, 
			Action action, State endState) {
		Transition trans = new DefaultTransition(beginState, cond, action, endState);
		transitions.add(trans);
		return trans;
	}
	
	
	private String createActionName(Activity act) {
		return act.getName().replace(" ", "_"); 
	}

	
	private Transition addActionToSTS(Action action, State startState, State endState, 
			StateFormula cond) {
		
		if (action.isInputAction()) {
			inputActions.add(action);
		} else {
			outputActions.add(action);
		}
		
		Transition trans = createTransition(startState, cond, action, endState);
		return trans;
	}
	
	private STS transformAdaptationModel(Correction correction, int index) {
		Adaptation adaptation = correction.getAdaptation();
		ProcessModel model = adaptation.getAdaptationModel();
		StateFormula cond = correction.getCondition();
		STS adaptSTS = this.getBasicAdaptationSTS(model);
		
		Action resume = new DefaultAction("Resume" + index, true, model);
		this.addResumeToAdaptationSTS(resume, adaptSTS, adaptation.getPreconditionOfToNode());
		
		for (Transition trans : adaptSTS.getTransitionsFromState(
				adaptSTS.getInitialState())) {
			trans.setCondition(cond);
			trans.addGuardClause(
					new DefaultClause(new DefaultLiteral("Trace" + index)));
			trans.addGuardClause(
					new DefaultClause(new DefaultLiteral("Point" + index)));
		}
		
		if (adaptSTS.getStates().size() == 1) {
			adaptSTS.addState(new DefaultState("dummyState"));
		}
		
		this.encodeJumps(adaptation, index, resume);		
		
		return adaptSTS;
	}
	
	private void addResumeToAdaptationSTS(Action resume, STS adaptSTS, StateFormula cond) {
		adaptSTS.addAction(resume);
		
		for (State state : adaptSTS.getFinalStates()) {
			Transition transOnResume = new DefaultTransition(state, cond, resume, 
					adaptSTS.getInitialState());
			adaptSTS.addTransition(transOnResume);
		}
	}
	
	private void encodeJumps(Adaptation adaptation, int index, Action resume) {
		ActivityNode fromNode = adaptation.getFromNode();
		ActivityNode toNode = adaptation.getToNode();
		Transition fromTrans = model2sts.getCorrespondingTransition(fromNode);
		Transition toTrans = model2sts.getCorrespondingTransition(toNode);
		
		ProcessModel fromModel = (ProcessModel) fromTrans.getAction().getRelatedEntity();
		logger.debug("Encoding jump from node " + fromNode.getNodeID() + " with activity " + 
				fromNode.getActivityName() + " from model " + fromModel.getName());

		State jumpStartState = fromTrans.getTarget();
		Set<State> mainProcessJumpStartStates = new HashSet<State>();
		
		if (this.originalModel.equals(fromModel)) { 
			logger.info("Encoding adaptation on original model");
			mainProcessJumpStartStates.add(jumpStartState);
			mainSTS.labelState(jumpStartState, "Point" + index);
			
			
		} else {
			for (Adaptation ad : this.adaptationSTSs.keySet()) {
				if (ad.getAdaptationModel().getName().equals(fromModel.getName())) {
					logger.info("Encoding adaptation on a previous adaptation");
					
					STS fromAdaptSTS = this.adaptationSTSs.get(ad);
					Integer fromAdaptIndex = this.adaptationIndexes.get(ad); 
					
					mainProcessJumpStartStates.addAll(
							mainSTS.getStatesForLabel("Point" + fromAdaptIndex));
					
					fromAdaptSTS.labelState(jumpStartState, "Point" + index);
					Transition adJumpTrans = new DefaultTransition(jumpStartState, resume, 
							fromAdaptSTS.getInitialState());
					fromAdaptSTS.addTransition(adJumpTrans);
					break;
				}
			}
		}
		
		for (State state : mainProcessJumpStartStates) {
			Transition jumpTrans = new DefaultTransition(state, resume,
					toTrans.getSource());
			this.mainSTS.addTransition(jumpTrans);
		}

	}
	
	private STS getBasicAdaptationSTS(ProcessModel model) {		
		// for adaptation models the start node is undefined
		ProcessNode start = null;
		for (ProcessNode node : model.getProcessNodes()) {
			if (model.incomingEdgesOf(node).isEmpty()) {
					start = node;
					break;
			}
		}
		return model2sts.transformProcessModel(model, start);
	}
	
	
	private STS transformTrace(Trace trace, Collection<ProcessModel> onModels, int index) {
		init();
		State initialState = createState();	
		cState = initialState;
		
		if (!trace.isEmpty()) {
			Map<ProcessModel, Set<Action>> noActivityInputActions = this.getNoActivityInputActions();
			State outState = createState("tout");
			
			for (int i = 0; i < trace.size(); i++) {
				Activity act = trace.getActivity(i);
				Activity prevAct = null;
				if (i > 0) {
					prevAct = trace.getActivity(i - 1);
				}
				State nextState = transformTraceActivity(act, prevAct, outState, onModels, noActivityInputActions);
				
				if (nextState != null) {
					cState = nextState;
				}
			}

			cState.setName("tend");
			for (ProcessModel model : processModels) {
				Set<Action> exceptions = noActivityInputActions.get(model);
				if (exceptions == null) {
					exceptions = new HashSet<Action>();
				}
				Action otherwiseAct = new WildcardAction(true, model, exceptions);
				addActionToSTS(otherwiseAct, cState, outState, StateFormula.getTop());
			}
		}
		
		STS traceSTS = new DefaultSTS(states, initialState, inputActions, 
				outputActions, transitions);	
		traceSTS.labelState(cState, "Trace" + index);
		return traceSTS;
	}
	
	
	private State transformTraceActivity(Activity act, Activity prevAct, State outState, 
			Collection<ProcessModel> onModels, Map<ProcessModel, Set<Action>> noActivityInputActions) {
		
		Map<ProcessModel, Set<Action>> modelActions = new HashMap<ProcessModel, Set<Action>>();
		
		State nextState = null;
		for (ProcessModel model : onModels) {
			Set<Action> actions = new HashSet<Action>();
			actions.addAll(noActivityInputActions.get(model));
				
			if (act.appearsAsNode(model)) {
				// trace can jump from a process model to another, and in that case the activities
				// do not have to be consecutive
				if (prevAct != null && prevAct.appearsAsNode(model) &&
						!act.followsDirectly(prevAct, model) && !act.isFirstNode(model)) {
					continue;
				}
				if (nextState == null) {
					nextState = createState(); 
				}
				
				Action action = new DefaultAction(createActionName(act), true, act, model);
				actions.add(action);
				addActionToSTS(action, cState, nextState, act.getPrecondition());	
			}
			if (!actions.isEmpty()) {
				modelActions.put(model, actions);
			}
		}
		
		for (ProcessModel model : processModels) {
			Set<Action> exceptions = modelActions.get(model);
			if (exceptions != null) {
				Action otherwiseAct = new WildcardAction(true, model, exceptions);
				addActionToSTS(otherwiseAct, cState, outState, StateFormula.getTop());
			}
		}
		return nextState;
	}
	
	
	private Action getCorrespondingResumeAction(ProcessModel model) {
		for (Adaptation ad : this.adaptationSTSs.keySet()) {
			if (ad.getAdaptationModel().equals(model)) {
				STS sts = this.adaptationSTSs.get(ad);
				for (Action action : sts.getInputActions()) {
					if (action.getName().startsWith("Resume")) {
						return action;
					}
				}
				break;
			}
		}
		return null;
	}
	

	private Map<ProcessModel, Set<Action>> getNoActivityInputActions() {
		Map<ProcessModel, Set<Action>> noActivityInputActions = new HashMap<ProcessModel, Set<Action>>();
		Set<Action> mainSTSActions = new HashSet<Action>();
		for (Action action : mainSTS.getInputActions()) {
			if (!action.isRelatedToAnActivity() && !action.getName().startsWith("Resume")) {
				mainSTSActions.add(action);
			}
		}
		noActivityInputActions.put(originalModel, mainSTSActions);
		
		for (Adaptation ad : this.adaptationSTSs.keySet()) {
			STS sts = this.adaptationSTSs.get(ad);
			Set<Action> adaptActions = new HashSet<Action>();
			for (Action action : sts.getInputActions()) {
				if (!action.isRelatedToAnActivity() && !action.getName().startsWith("Resume")) {
					adaptActions.add(action);
				}
			}
			noActivityInputActions.put(ad.getAdaptationModel(), adaptActions);
		}
		return noActivityInputActions;
	}

	
	public Map<Action, Activity> getActionTable() {
		return model2sts.getActionTable();
	}

	public STS getConditionSTS(Condition properCond) {
		for (Condition cond : conditionSTSs.keySet()) {
			if (cond.equals(properCond)) {
				return conditionSTSs.get(cond);
			}
		}
		return null;
	}

	public STS getDomainObjectSTS(DomainObject object) {
		return this.objectSTSs.get(object);
	}


	public STS getProcessModelSTS(ProcessModel model) {
		if (model.equals(originalModel)) {
			return mainSTS;
		}
		
		for (Adaptation ad : this.adaptationSTSs.keySet()) {
			if (model.equals(ad.getAdaptationModel())) {
				return this.adaptationSTSs.get(ad);
			}
		}
		return null;
	}

	public Map<Action, Condition> getActionConditionCorrespondences() {
		return action2cond;
	}	
	
}
