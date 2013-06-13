package eu.fbk.soa.evolution.engine.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import eu.fbk.soa.evolution.sts.Action;
import eu.fbk.soa.evolution.sts.Clause;
import eu.fbk.soa.evolution.sts.Literal;
import eu.fbk.soa.evolution.sts.STS;
import eu.fbk.soa.evolution.sts.State;
import eu.fbk.soa.evolution.sts.Transition;
import eu.fbk.soa.evolution.sts.impl.WildcardAction;
import eu.fbk.soa.process.ProcessModel;
import eu.fbk.soa.process.StateFormula;
import eu.fbk.soa.process.StateLiteralClause;
import eu.fbk.soa.process.domain.DomainObject;
import eu.fbk.soa.process.domain.StateLiteral;
import eu.fbk.soa.util.StringUtils;

public class STSToSMV {
	
	private Map<ProcessModel, String> modelIDs;
	
	private Map<DomainObject, String> objIDs;
		
	private Map<Condition, String> condIDs;
	
	private Map<String, STS> id2STS;
	
	public STSToSMV(Map<ProcessModel, String> processModelIDs,
			Map<DomainObject, String> objects, Map<Condition, String> condIDs,
			Map<String, STS> id2STS) {
		
		this.modelIDs = processModelIDs;
		this.objIDs = objects;
		this.condIDs = condIDs;
		this.id2STS = id2STS;
	}
	
	public String translateSTS(STS sts) {
		String translation = "VAR state: {"
				+ StringUtils.getCommaSeparatedString(sts.getStates()) + "};\n";
		
		if (sts.getInitialState() != null) {
			translation += "INIT state = " + sts.getInitialState() + "\n\n";
		}
		
		if (!sts.getTransitions().isEmpty()) {
			translation += "ASSIGN next(state) :=\n  case\n";
			for (Transition tr : sts.getTransitions()) {
				translation += translateTransition(tr, new Object());
			}
			translation += "TRUE : state;\n esac; \n\n";
		}
		
		for (Action action : sts.getActions()) {
//			if (!action.getRelatedProcessModel().equals(translatedEntity)) {
//				continue;
//			}
			String stateCond = "";
			for (Transition trans : sts.getTransitions()) {
				if (trans.getAction() == action) {
					if (!stateCond.isEmpty()) {
						stateCond += " | ";
					}
					stateCond += "(state = " + trans.getSource() + " ";
					String formula = translateStateFormula(trans.getCondition());
					if (formula != "") {
						stateCond += "& " + formula;
					}
					stateCond += ")";
				}
			}
			if (!stateCond.isEmpty()) {
				translation += "TRANS " + this.translateAction(action, new Object()) + " & !("
					+ stateCond + ") -> FALSE\n";
			}
		}

		translation += "\n";
		return translation;
		
	}
	
	public String translateSTS(STS sts, Object translatedEntity) {
		
		String input = getActionDeclaration(sts.getInputActions(), translatedEntity, "input");
		String output = getActionDeclaration(sts.getOutputActions(), translatedEntity, "output");
		
		String translation = input + output;
		
		translation += "VAR state: {"
				+ StringUtils.getCommaSeparatedString(sts.getStates()) + "};\n";
		
		if (sts.getStates().size() > 1 && sts.getInitialState() != null) {
			translation += "INIT state = " + sts.getInitialState() + "\n\n";
		}
		
		if (sts.getTransitions().size() > 0) {
			translation += "ASSIGN next(state) :=\n  case\n";
			translation += translateTransitions(sts, translatedEntity);
			translation += "TRUE : state;\n esac; \n\n";
		}
		
		for (Action action : sts.getActions()) {
			String stateCond = "";
			
			if (!action.getRelatedEntity().equals(translatedEntity)) {
				continue;
			}
			for (Transition trans : sts.getTransitions()) {
				if (trans.getAction().equals(action) || 
						(action.getName().equals("UNDEF") && !trans.getAction().getRelatedEntity().equals(translatedEntity))) {
					
					if (!stateCond.isEmpty()) {
						stateCond += " | ";
					}
//					stateCond += "(state = " + trans.getSource() + " ";
//					stateCond += translateStateFormula(trans.getCondition()) + ")";
					
					stateCond += translateTransitionCondition(trans, translatedEntity);
					
				}
			}
			if (!stateCond.isEmpty()) {
				translation += "TRANS " + this.translateAction(action, translatedEntity) + " & !("
					+ stateCond + ") -> FALSE\n";
			}
		}
		translation += "\n";
		return translation;
	}
	
	
	private String translateTransitions(STS sts, Object translatedEntity) {
		String translation = "";
		Set<Set<Transition>> transitionGroups = new HashSet<Set<Transition>>();
		
		for (State state : sts.getStates()) {
//			System.out.println("Looking at state " + state);
			Set<Transition> transitions = 
				sts.getTransitionsFromState(state);
//			System.out.println(transitions.size() + " outgoing transitions ");
			
			List<Transition> remainingTrans = new ArrayList<Transition>(transitions);
			
			while (!remainingTrans.isEmpty()) {
				Transition trans1 = remainingTrans.remove(0);
				Set<Transition> newGroup = new HashSet<Transition>();
				newGroup.add(trans1);
				
				for (Transition trans2 : transitions) {
					if (!trans1.getTarget().equals(trans2.getTarget()) && 
							trans1.getAction().equals(trans2.getAction()) && 
							trans1.getCondition().equals(trans2.getCondition())) {
						newGroup.add(trans2);
						remainingTrans.remove(trans2);
					}
				}
				transitionGroups.add(newGroup);
			}
		}
			
		for (Set<Transition> trGroup : transitionGroups) {
			translation += translateTransitionGroup(trGroup, translatedEntity);
		}
		return translation;
	}


	private String getActionDeclaration(Set<Action> totalActions, 
			Object translatedEntity, String type) {
		String translation = "";
		
		Set<Action> actions = new HashSet<Action>(totalActions);
		for (Action act : totalActions) {
			if (!act.getRelatedEntity().equals(translatedEntity)) {
				actions.remove(act);
			}
		}
		if (!actions.isEmpty()) { 
			translation += "IVAR " + type + ": {UNDEF";
			Set<Action> noUndefActions = new HashSet<Action>(totalActions);
			for (Action act : totalActions) {
				if (act.getName().equals("UNDEF")) {
					noUndefActions.remove(act);
				}
			}
			String actionsDecl = StringUtils.getCommaSeparatedString(noUndefActions);
			if (actionsDecl != "") {
				translation += ", " + actionsDecl;
			}
			translation += "};\n";
		}
		return translation;
	}
	
	private String translateTransitionGroup(Set<Transition> trGroup,
			Object translatedEntity) {
		
		String translation = "";
		if (trGroup.size() >= 1) {
			Transition trans = trGroup.iterator().next();
			
			translation +=
				this.translateTransitionCondition(trans, translatedEntity);
			translation += "& " + translateAction(trans.getAction(), translatedEntity) + " ";	
			
			translation += ": ";
			
			int nr = 0;
			String targetStates = "";
			for (Transition t : trGroup) {
				State target = t.getTarget();
				if (nr > 0) {
					targetStates += ", ";
				}
				targetStates += target.getName();
				nr++;
			}
			if (nr == 1) {
				translation += targetStates;
			} else {
				translation += "{" + targetStates + "}";
			}
			
			translation += ";\n";
		}
		return translation;
	}
	
	private String translateTransition(Transition trans, Object translatedEntity) {
		String translation = 
			this.translateTransitionCondition(trans, translatedEntity);
		
		translation += "& " + translateAction(trans.getAction(), translatedEntity) + " ";	
		
		translation += ": " + trans.getTarget() + ";\n";
		return translation;	
	}
	
	private String translateTransitionCondition(Transition trans, Object translatedEntity) {
		String translation = "(state = " + trans.getSource() + ") ";
		
		String formula = translateStateFormula(trans.getCondition());
		if (formula != "") {
			translation += "& " + formula;
		}
		
		for (Clause<? extends Literal> clause : trans.getGuardClauses()) {
			String clauseCond = "";
			Iterator<? extends Literal> iterator = clause.getLiterals().iterator();
			
			while (iterator.hasNext()) {
				Literal lit = iterator.next();
				String guardCond = "";
				if (lit.isNegated()) {
					guardCond += "!(";
				}
				for (String entityID : id2STS.keySet()) {
					STS sts = id2STS.get(entityID);

					Set<State> states = sts.getStatesForLabel(lit.getProposition());
					for (State state : states) {
						if (guardCond != "" && !guardCond.equals("!(")) {
							guardCond += " | ";
						}
						guardCond += entityID + ".state = " + state.getName();
					}
				}
				if (lit.isNegated()) {
					guardCond += ")";
				}
				if (guardCond != "") {
					clauseCond += guardCond;
					if (iterator.hasNext()) {
						clauseCond += " | ";
					}
				}
			}
			if (clauseCond != "") {
				translation += "& (" + clauseCond + ") ";
			}
		}
		return translation;
	}
	
	public String translateStateFormula(StateFormula formula) {
		String translation = "";
		if (!formula.isEmpty()) {
			for (StateLiteralClause cl : formula.getClauses()) {
				if (translation != "") {
					translation += "& (";
				} else {
					translation = "(";
				}
				
				Iterator<StateLiteral> iterator = cl.getLiterals().iterator();
				while (iterator.hasNext()) {
					StateLiteral lit = iterator.next();
					String objID = objIDs.get(lit.getDomainObject());
					String inState = objID + ".state = " + lit.getState();
					if (lit.isNegated()) {
						translation += "!(" + inState + ")";
					} else {
						translation += inState;
					}
							
					if (iterator.hasNext()) {
						translation += " | ";
					}
				}
				translation += ") ";
			}
		}
		if (formula.isNegated() && !formula.isEmpty()) {
			translation = "!(" + translation + ")";
		}
		return translation;
	}
	
	private String translateAction(Action action, Object translatedEntity) {
		String var = "";
		Object relatedEntity = action.getRelatedEntity();
		if (!relatedEntity.equals(translatedEntity)) {
			if (relatedEntity instanceof ProcessModel) {
				var += modelIDs.get((ProcessModel) relatedEntity) + ".";
			} 
			if (relatedEntity instanceof Condition) {
				var += condIDs.get((Condition) relatedEntity) + ".";
			}
		}
		if (action.isInputAction()) {
			var += "input";
		} else {
			var += "output";
		}
		
		String translation = "";
		if (action instanceof WildcardAction) {
			translation += "!(" + var + " = UNDEF)";
			
			for (Action exc : ((WildcardAction) action).getDifferentFrom()) {
				translation += " & !(" + var + " = " + exc.getName() + ")";
			}
		} else {
			translation += "(" + var + " = " + action.getName() + ")";
		}
		
		return translation;
	}	
}
