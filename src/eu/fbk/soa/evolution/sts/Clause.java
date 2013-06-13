package eu.fbk.soa.evolution.sts;

import java.util.Set;



public interface Clause<LiteralType extends Literal> {

	public Set<LiteralType> getLiterals();
	
	public void removeLiteral(LiteralType literal);
	
}
