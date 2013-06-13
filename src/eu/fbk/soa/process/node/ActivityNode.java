package eu.fbk.soa.process.node;

import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import eu.fbk.soa.process.Activity;
import eu.fbk.soa.process.domain.DomainObject;

@XmlAccessorType(XmlAccessType.NONE)
public class ActivityNode extends ProcessNode {

	@XmlAttribute(name="activity", required=true)
	private String activityName = "";
	
	Activity activity;
	
	public ActivityNode() {
		super();
	}
	
	public String getActivityName() {
		return activityName;
	}

	public ActivityNode(String name) {
		super();
		this.activityName = name;
		this.activity = Activity.getActivity(name);
	}
	
	public ActivityNode(Activity activity) {
		super();
		this.activity = activity;
		this.activityName = activity.getName();
	}
	
	public String toString() {
		return activity.toString();
	}

	public boolean pointsTo(Activity act) {
		return (this.activity.equals(act));
	}
	
	public Activity getActivity() {
		return this.activity;
	}
	
	public Set<DomainObject> getRelatedDomainObjects() {
		return activity.getRelatedDomainObjects();
	}
	
	public boolean equals(Object object) {
		if (object instanceof ActivityNode) {
			ActivityNode actNode = (ActivityNode) object;
			if ((this.activity == null || actNode.getActivity() == null) && 
					!(this.activity == null && actNode.getActivity() == null)) {
				return false;
			}
			if (this.nodeNr != actNode.nodeNr) {
				return false;
			}
			if (this.activity != null && actNode.getActivity() != null && 
					!this.activity.equals(actNode.getActivity())) {
				return false;
			}
			return true;
		}
		return false;
	}

	public void setActivity(Activity act) {
		this.activity = act;
	}
	
	public int hashCode() {
		return 0;
	}
}
