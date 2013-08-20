package eu.fbk.soa.process.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.PropertyConfigurator;
import org.junit.Before;
import org.junit.Test;

import eu.fbk.soa.process.StateFormula;
import eu.fbk.soa.process.StateLiteralClause;
import eu.fbk.soa.process.domain.DomainObject;
import eu.fbk.soa.process.domain.ObjectEvent;
import eu.fbk.soa.process.domain.ObjectState;
import eu.fbk.soa.process.domain.ObjectTransition;
import eu.fbk.soa.process.domain.StateLiteral;

public class StateFormulaTest {
	
	private StateFormula formula;

	private List<StateLiteral> literals;	
	
	/*
	 * Setting up a domain object Obj1 with 3 states: {state0, state1, state2}
	 * and the formula:  state0(Obj1) | not(state1(Obj1))
	 */
	@Before
	public void setUpFormula() {
		PropertyConfigurator.configure("log4j.properties");

		literals = new ArrayList<StateLiteral>();
		List<ObjectState> states = new ArrayList<ObjectState>();
		Set<ObjectState> initialStates = new HashSet<ObjectState>();

		for (int i = 0; i < 3; i++) {
			ObjectState state = new ObjectState("state" + i);
			states.add(state);
			if (i == 0) {
				initialStates.add(state);
			}
		}
		
		DomainObject obj1 = new DomainObject("Obj1", states,
				initialStates, new HashSet<ObjectEvent>(), new HashSet<ObjectTransition>());

		for (int i = 0; i < 3; i++) {
			StateLiteral lit = new StateLiteral(obj1, states.get(i));
			literals.add(lit);
		}
		
		formula = new StateFormula(new StateLiteralClause(
				literals.get(0), new StateLiteral(obj1, states.get(1), true)));
	}

	@Test
	public void testUnitPropagation() {
		StateLiteralClause unitClause = new StateLiteralClause(literals.get(0));
		formula.addClause(unitClause);

		assertEquals("Through unit propagation, the first clause should be removed", 
				1, formula.getNumberOfClauses());
		assertTrue("Through unit propagation, the first clause should be removed",
				formula.containsClause(unitClause));
	}

	@Test
	public void testGetEquivalentFormulaWithoutNegations() {

		StateFormula noNegationFormula = formula.getEquivalentFormulaWithoutNegations();

		StateLiteralClause firstClause = noNegationFormula.getClauses().get(0);
		assertTrue("In the clause without negations not(state1(Obj1)) should be " +
				"replaced with state0(Obj1) | state2(Obj1)", firstClause.containsLiteral(literals.get(2)));

		assertEquals("In the clause without negations state0(Obj1) should appear only once",
				2, firstClause.getNumberOfLiterals());
	}

}
