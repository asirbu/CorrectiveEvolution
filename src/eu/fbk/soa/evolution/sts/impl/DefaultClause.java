package eu.fbk.soa.evolution.sts.impl;

import java.util.HashSet;
import java.util.Set;

import eu.fbk.soa.evolution.sts.Clause;

public class DefaultClause implements Clause<DefaultLiteral> {

	private Set<DefaultLiteral> literals = new HashSet<DefaultLiteral>();

	public DefaultClause(DefaultLiteral prop) {
		this.addLiteral(prop);
	}

	public void addLiteral(DefaultLiteral prop) {
		literals.add(prop);
	}

	public DefaultClause(DefaultLiteral...props) {
		for (DefaultLiteral p : props) {
			this.literals.add(p);
		}
	}
	
	public DefaultClause(Set<DefaultLiteral> lits) {
		this.literals.addAll(lits);
	}
	
	
	@Override
	public Set<DefaultLiteral> getLiterals() {
		return literals;
	}

	@Override
	public void removeLiteral(DefaultLiteral literal) {
		literals.remove(literal);
	}

}
