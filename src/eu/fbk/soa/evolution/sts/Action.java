package eu.fbk.soa.evolution.sts;


public interface Action {

	public String getName();

	public boolean isInputAction();

	public Object getRelatedEntity();

	public boolean isRelatedToAnActivity();
		
}
