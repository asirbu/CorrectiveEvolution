package eu.fbk.soa.process;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import eu.fbk.soa.process.domain.DomainObject;
import eu.fbk.soa.process.domain.ObjectState;
import eu.fbk.soa.process.domain.StateLiteral;

public class StateLiteralClauseTest {

	@Test
	public void testStringRepresentation() {
		DomainObject obj1 = new DomainObject("Object_1");
		DomainObject obj2 = new DomainObject("Object_2");

		ObjectState state1 = new ObjectState("s1");
		ObjectState state2 = new ObjectState("s2");

		StateLiteral lit1 = new StateLiteral(obj1, state1);
		StateLiteral lit2 = new StateLiteral(obj2, state2, true);

		StateLiteralClause clause = new StateLiteralClause(lit1, lit2);
		String clauseStr = clause.toString();

		assertTrue(clauseStr.contains("s1(Object_1)"));
		assertTrue(clauseStr.contains("not(s2(Object_2))"));
	}

}
