package eu.fbk.soa.evolution.sts.impl;

import eu.fbk.soa.evolution.sts.Action;
import eu.fbk.soa.process.Activity;
import eu.fbk.soa.process.ProcessModel;


public class DefaultAction implements Action {

	private String name = "";

	private boolean isInput;
	
	private Activity relatedActivity;
	
	private Object relatedEntity;

	public DefaultAction() {};
	
	public DefaultAction(String name, boolean isInput, Activity relatedAct, ProcessModel model) {
		this.name = name;
		this.isInput = isInput;
		this.relatedActivity = relatedAct;
		this.relatedEntity = model;
	}

	public DefaultAction(String name, boolean isInput, Object entity) {
		this.name = name;
		this.isInput = isInput;
		this.relatedEntity = entity;
	}
	
	public DefaultAction(String name, boolean isInput) {
		this.name = name;
		this.isInput = isInput;
	}
	
	@Override
	public String getName() {
		return name;
	}

	public String toString() {
		return name;
	}

	/* (non-Javadoc)
	 * @see evolution.sts.IAction#isInputAction()
	 */
	@Override
	public boolean isInputAction() {
		return isInput;
	}

	public Activity getRelatedActivity() {
		
		return relatedActivity;
	}

	public Object getRelatedEntity() {
		return this.relatedEntity;
	}
	
	public boolean equals(Object obj) {
		if (!(obj instanceof DefaultAction)) {
			return false;
		}
		DefaultAction action = (DefaultAction) obj;
		if (!action.getName().equals(this.name) ||
				(action.isInputAction() != this.isInput)) {
			return false;
		}
		
		if (!hasSameRelatedActivity(action) || !hasSameRelatedEntity(action)) {
			return false;
		}
		return true;
	}
	
	private boolean hasSameRelatedActivity(DefaultAction action) {
		Activity activity = action.getRelatedActivity();
		if (relatedActivity == null) {
			if (activity != null) {
				return false;
			}
		} else {
			if (activity == null || !activity.equals(this.relatedActivity)) {
				return false;
			}
		}
		return true;
	}
	
	private boolean hasSameRelatedEntity(DefaultAction action) {
		Object entity = action.getRelatedEntity();
		if (relatedEntity == null) {
			if (entity != null) {
				return false;
			}
		} else {
			if (entity == null || !entity.equals(this.relatedEntity)) {
				return false;
			}
		}
		return true;
	}
	
	public int hashCode() {
		return this.name.hashCode();
	}

	@Override
	public boolean isRelatedToAnActivity() {
		return (this.relatedActivity != null);
	}
}
