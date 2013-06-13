package eu.fbk.soa.evolution.sts.impl;

import java.util.HashSet;
import java.util.Set;

import eu.fbk.soa.evolution.sts.Action;
import eu.fbk.soa.process.ProcessModel;

public class WildcardAction implements Action {
	
	private static int nrActions = 1;
	
	private String name;
	
	private boolean isInput;
	
	private ProcessModel relatedModel;
	
	private Set<Action> differentFrom;

	public WildcardAction(boolean isInput, ProcessModel model) {
		this(isInput, model,  new HashSet<Action>());
	}
	
	public WildcardAction(boolean isInput, ProcessModel model, 
			Set<Action> exceptions) {
		this.name = "wildcard" + nrActions;
		nrActions++;
		this.isInput = isInput;
		this.relatedModel = model;
		this.differentFrom = exceptions;
	}
	
	
	@Override
	public String getName() {
		return name;
	}

	@Override
	public boolean isInputAction() {
		return isInput;
	}

	@Override
	public ProcessModel getRelatedEntity() {
		return relatedModel;
	}

	public Set<Action> getDifferentFrom() {
		return this.differentFrom;
		
	}

	@Override
	public boolean isRelatedToAnActivity() {
		return false;
	}

}
