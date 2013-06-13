package eu.fbk.soa.evolution.sts.impl;

import eu.fbk.soa.evolution.sts.Literal;

public class DefaultLiteral implements Literal {

	private boolean isNegated = false;
	
	private String proposition;
	
	public DefaultLiteral(String prop, boolean isNegated) {
		this.proposition = prop;
		this.isNegated = isNegated;
	}
	
	public DefaultLiteral(String prop) {
		this.proposition = prop;
	}
		
	
	@Override
	public boolean isNegated() {
		return isNegated;
	}

	@Override
	public void negate() {
		isNegated = !isNegated;
	}

	@Override
	public DefaultLiteral getNegation() {
		return new DefaultLiteral(this.proposition, !this.isNegated);
	}

	@Override
	public String getProposition() {
		return proposition;
	}
	
	
	
	

}
