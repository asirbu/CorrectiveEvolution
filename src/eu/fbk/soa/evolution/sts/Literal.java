package eu.fbk.soa.evolution.sts;

public interface Literal {

	public boolean isNegated();
	
	public void negate();
	
	public Literal getNegation();

	public String getProposition();
	
}
