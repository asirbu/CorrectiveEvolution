package eu.fbk.soa.process;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlElements;

import org.apache.log4j.Logger;

import eu.fbk.soa.process.domain.DomainObject;
import eu.fbk.soa.process.domain.ObjectState;
import eu.fbk.soa.process.domain.StateLiteral;



/**
 * State formulas a boolean formulas over domain object states. 
 */
public class StateFormula {

	private static Logger logger = Logger.getLogger(StateFormula.class);
	
	protected boolean isNegated = false;
	
	@XmlElementWrapper(name = "and", namespace = "http://soa.fbk.eu/CNFFormula")
	@XmlElements(
			@XmlElement(name = "orClause", namespace = "http://soa.fbk.eu/CNFFormula"))	
	private List<StateLiteralClause> clauses;

	public StateFormula() {
		this.clauses = new ArrayList<StateLiteralClause>();
	}

	public StateFormula(StateLiteral sp) {
		this.clauses = new ArrayList<StateLiteralClause>();
		addClause(new StateLiteralClause(sp));
	}
	
	public StateFormula(StateLiteral...stateLits) {
		this.clauses = new ArrayList<StateLiteralClause>();
		for (StateLiteral sLit : stateLits) {
			addClause(new StateLiteralClause(sLit));
		}
	}

	public StateFormula(StateLiteralClause clause) {
		this.clauses = new ArrayList<StateLiteralClause>();
		this.clauses.add(clause);
	}
	
	public StateFormula(StateLiteralClause...clausesArray) {
		this.clauses = new ArrayList<StateLiteralClause>();
		for (StateLiteralClause clause : clausesArray) {
			this.clauses.add(clause);
		}
	}

	public StateFormula(Collection<StateLiteralClause> clauses) {
		this.clauses = new ArrayList<StateLiteralClause>(clauses);
	}


	public void addClause(StateLiteralClause clause) {
		if (this.containsClause(clause)) {
			return;
		}
		this.clauses.add(clause);
		this.unitPropagation();
	}

	private void unitPropagation() {
		Set<StateLiteral> literals = new HashSet<StateLiteral>();
		for (StateLiteralClause clause : this.clauses) {
			if (clause.isUnitClause()) {
				literals.add(clause.getFirstLiteral());
			}
		}
		for (StateLiteral literal : literals) {
			propagateLiteral(literal);			
		}
	}

	private void propagateLiteral(StateLiteral literal) {
		StateLiteral negation = literal.getNegation();
		Set<StateLiteralClause> clausesCopy = new HashSet<StateLiteralClause>(this.clauses);
		
		for (StateLiteralClause clause : clausesCopy) {
			if (!clause.isUnitClause()) {
				if (clause.containsLiteral(literal)) {
					this.clauses.remove(clause);
				} else {
					if (clause.containsLiteral(negation)) {
						StateLiteralClause newClause = clause.getCopy();
						newClause.removeLiteral((StateLiteral) negation);
						this.clauses.add(newClause);
						this.clauses.remove(clause);
					}
				}
			}
		}
	}
	
	public List<StateLiteralClause> getClauses() {
		return clauses;
	}

	public int getNumberOfClauses() {
		return clauses.size();
	}

	public boolean equalsTop() {
		return (clauses.size() == 0);
	}

	public boolean isTriviallyFalse() {
		for (StateLiteralClause c : clauses) {
			if (c.isEmpty()) {
				return true;
			}
		}
		return false;
	}
	
	public String toString() {
		if (this.isEmpty()) {
			return "T";
		}
		String toString = "";
		for (int i = 0; i < clauses.size(); i++) {
			if (i > 0) {
				toString += " & ";
			}
			toString += clauses.get(i).toString();		
		}
		if (isNegated) {
			return "not(" + toString + ")";
		} else {
			return toString;
		}
	}
	
	public int hashCode() {
		return this.toString().hashCode();
	}
	
	public boolean equals(Object obj) {
		if (!(obj instanceof StateFormula)) {
			return false;
		}
		StateFormula formula = (StateFormula) obj;
		for (StateLiteralClause c : this.clauses) {
			if (!formula.containsClause(c)) {
				return false;
			}
		}
		for (StateLiteralClause c : formula.getClauses()) {
			if (!this.containsClause(c)) {
				return false;
			}
		}
		if (this.isNegated != formula.isNegated()) {
			return false;
		}
		return true;
	}
	
	public boolean isEmpty() {
		return this.clauses.isEmpty();
	}

	public void add(StateFormula formula) {
		for (StateLiteralClause c : formula.getClauses()) {
			this.addClause(c);
		}
	}

	public boolean containsClause(StateLiteralClause clause) {
		
		for (StateLiteralClause c : clauses) {
			boolean matches = true;
			for (StateLiteral l : c.getLiterals()) {
				if (!clause.containsLiteral(l)) {
					//System.out.println("Clause " + clause + " does not contain " + l);
					matches = false;
					break;
				}
			}
			if (matches) {
				for (StateLiteral l : clause.getLiterals()) {
					if (!c.containsLiteral(l)) {
						//System.out.println("Clause " + c + " does not contain " + l);
						matches = false;
						break;
					}
				}
			}
			if (matches) {
				return true;
			}
		}
		return false;
		
	}

	public void updateObjectReferences(Set<DomainObject> objects) {
		for (StateLiteralClause c : clauses) {
			c.updateObjectReferences(objects);
		}
	}
	
	public Set<DomainObject> getRelatedDomainObjects() {
		Set<DomainObject> objects = new HashSet<DomainObject>();
		for (StateLiteralClause clause : clauses) {
			objects.addAll(clause.getRelatedDomainObjects());
		}
		
		return objects;
	}
	
	public boolean isRelatedTo(DomainObject obj) {
		for (StateLiteralClause c : this.getClauses()) {
			if (c.isRelatedTo(obj)) {
				return true;
			}
		}
		return false;
	}
	

	public void negate() {
		this.isNegated = !(this.isNegated);
	}
	
	public boolean isNegated() {
		return this.isNegated;
	}
	
	public boolean hasUnresolvedReferences() {
		for (StateLiteralClause c : clauses) {
			if (c.hasUnresolvedReferences()) {
				return true;
			}
		}
		return false;
	}
	
	boolean hasOnlyUnitClauses() {
		for (StateLiteralClause clause : this.getClauses()) {
			if (!clause.isUnitClause()) {
				return false;
			}
		}
		return true;
	}
	
	boolean hasUnitClausesExceptOne() {
		int nonUnit = 0;
		for (StateLiteralClause clause : this.getClauses()) {
			if (!clause.isUnitClause()) {
				nonUnit++;
				if (nonUnit > 1) {
					return false;
				}
			}
		}
		return (nonUnit == 1);
	}

	
	public static StateFormula getTop() {
		return new StateFormula();
	}

	public StateFormula getNegation() {
		if (this.getClauses().size() == 1) {
			return this.negateOneClauseFormula(this);
		}
		if (this.hasOnlyUnitClauses()) {
			return this.negateUnitClausesFormula(this);
		}
		if (this.hasUnitClausesExceptOne()) {
			return this.negateUnitClausesExceptOneFormula(this);
		}
		StateFormula negatedFormula = new StateFormula(this.getClauses());
		if (!this.isNegated) {
			negatedFormula.negate();
		}
		return negatedFormula;
	}

	

	private StateFormula negateUnitClausesExceptOneFormula(
			StateFormula stateFormula) {
	
		List<StateLiteralClause> newClauses = new ArrayList<StateLiteralClause>();
		
		for (StateLiteralClause clause : stateFormula.getClauses()) {
			if (!clause.isUnitClause()) {
				for (StateLiteral lit : clause.getLiterals()) {
					StateLiteral negLit = lit.getNegation();
					StateLiteralClause newClause = new StateLiteralClause(negLit);
					newClauses.add(newClause);
				}
				break;
			}
		}
		
		for (StateLiteralClause clause : stateFormula.getClauses()) {
			if (clause.isUnitClause()) {
				StateLiteral lit = clause.getLiterals().iterator().next();
				StateLiteral negLit = lit.getNegation();
				for (StateLiteralClause newClause : newClauses) {
					newClause.addLiteral(negLit);
				}
			}
		}
			
		return new StateFormula(newClauses);
	}

	private StateFormula negateUnitClausesFormula(StateFormula stateFormula) {
		StateFormula negatedFormula = new StateFormula();
		StateLiteralClause newClause = new StateLiteralClause();
		
		for (StateLiteralClause clause : stateFormula.getClauses()) {
			for (StateLiteral lit : clause.getLiterals()) {
				StateLiteral negLit = lit.getNegation();
				newClause.addLiteral(negLit);
			}
		}
		negatedFormula.addClause(newClause);
		return negatedFormula;
	}

	
	private StateFormula negateOneClauseFormula(StateFormula formula) {
		StateFormula negatedFormula = new StateFormula();
		StateLiteralClause clause = this.getClauses().get(0);
		
		for (StateLiteral lit : clause.getLiterals()) {
			StateLiteralClause newClause = new StateLiteralClause();
			StateLiteral negLit = lit.getNegation();
			newClause.addLiteral(negLit);
			negatedFormula.addClause(newClause);
		}
		return negatedFormula;
	}
	

	public StateFormula getProjection(Set<DomainObject> domainObjects) {
		StateFormula projection = new StateFormula();
		for (StateLiteralClause clause : this.clauses) {
			StateLiteralClause newClause = new StateLiteralClause();
			for (StateLiteral lit : clause.getLiterals()) {
				if (domainObjects.contains(lit.getDomainObject())) {
					newClause.addLiteral(lit);
				}
			}
			if (!newClause.isEmpty()) {
				projection.addClause(newClause);
			}
		}
		return projection;
	}
	
	public StateFormula getEquivalentFormulaWithoutNegations() {
		StateFormula condNoNegations = new StateFormula();
		
		StateFormula notNegatedFormula = this;
		
		if (this.isNegated) {
			StateLiteralClause newClause = new StateLiteralClause(); 
			for (StateLiteralClause clause : clauses) {
				if (clause.getNumberOfLiterals() > 1) {
					throw new UnsupportedOperationException("Could not negate complex formula " + this);
				}
				newClause.addLiteral((StateLiteral) clause.getFirstLiteral().getNegation());
			}
			notNegatedFormula = new StateFormula(newClause);
			System.out.println("Introduced negation in formula " + notNegatedFormula);
		}
		
		for (StateLiteralClause clause : notNegatedFormula.getClauses()) {
			StateLiteralClause newClause = new StateLiteralClause();
			for (StateLiteral lit : clause.getLiterals()) {
				if (lit.isNegated()) {
					DomainObject obj = lit.getDomainObject();
					for (ObjectState state : obj.getStates()) {
						if (!state.equals(lit.getState())) {
							newClause.addLiteral(new StateLiteral(obj, state));
						}
					}
				} else {
					newClause.addLiteral(lit);
				}
			}
			condNoNegations.addClause(newClause);
			logger.debug("No negation formula " + condNoNegations);

		}
		return condNoNegations;
	}

	public static StateFormula createDisjunction(StateFormula formula1,
			StateFormula formula2) {
		
		StateFormula disjunction = new StateFormula();
		for (StateLiteralClause clause1 : formula1.getClauses()) {
			for (StateLiteralClause clause2 : formula2.getClauses()) {
				StateLiteralClause newClause = new StateLiteralClause(clause1.getLiterals());
				newClause.addLiterals(clause2.getLiterals());
				disjunction.addClause(newClause);
			}
		}
		return disjunction;
	}

}
