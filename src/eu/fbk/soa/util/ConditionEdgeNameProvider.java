package eu.fbk.soa.util;

import java.util.List;

import org.jgrapht.ext.EdgeNameProvider;

import eu.fbk.soa.process.ProcessEdge;
import eu.fbk.soa.process.StateFormula;
import eu.fbk.soa.process.StateLiteralClause;
import eu.fbk.soa.process.domain.ObjectLiteral;

public class ConditionEdgeNameProvider implements EdgeNameProvider<ProcessEdge> {

    public ConditionEdgeNameProvider() {
    }

	@Override
	public String getEdgeName(ProcessEdge edge) {
		String edgeName = "";
		StateFormula formula = edge.getCondition();
		if (formula != null && !formula.isEmpty()) {
			edgeName = formula.toString();
			if (edgeName.length() > 30) {
				edgeName = getFormulaWithBreaks(formula);
			}			
		}
		return edgeName;
	}
	
	private String getFormulaWithBreaks(StateFormula formula) {
		String toString = "";
		List<StateLiteralClause> clauses = formula.getClauses();
	
		for (int i = 0; i < clauses.size(); i++) {
			if (i > 0) {
				toString += " &\\n ";
			}
			toString += getClauseWithBreaks(clauses.get(i));		
		}
		if (formula.isNegated()) {
			return "not(" + toString + ")";
		} else {
			return toString;
		}
	}
	
	private String getClauseWithBreaks(StateLiteralClause clause) {
		if (clause.getLiterals().isEmpty()) {
			return "F";
		}
		String toString = "";
		for (ObjectLiteral l : clause.getLiterals()) {
			toString += l.toString() + " |\\n ";
		}
		if (toString.length() > 0) {
			toString = toString.substring(0, toString.length() - 3);
		} 
		if (clause.getLiterals().size() > 1) {
			return "(" + toString + ")";
		} else {
			return toString;
		}
	}
}
