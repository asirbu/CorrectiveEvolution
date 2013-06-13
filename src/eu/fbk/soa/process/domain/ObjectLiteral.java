package eu.fbk.soa.process.domain;

import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;

import eu.fbk.soa.evolution.sts.Literal;


@XmlAccessorType(XmlAccessType.NONE)
public abstract class ObjectLiteral implements Literal {

	DomainObject object;
	
	@XmlAttribute(name="objectID", required = true)
	String objectName;
	
	@XmlAttribute(name="isNegated", required = false)
	boolean isNegated = false;
	
	String completeName = "";
	
	public DomainObject getDomainObject() {
		return object;
	}
	
	public String getDomainObjectName() {
		return objectName;
	}
	
	public void setDomainObject(DomainObject object) {
		this.object = object;
		this.objectName = object.getName();
	}
	
	public boolean isRelatedTo(DomainObject domainObj) {
		return (objectName.equals(domainObj.getName()));
	}
	
	public void negate() {
		isNegated = !(isNegated);
	}
	
	public boolean isNegated() {
		return this.isNegated;
	}
	
	public abstract ObjectLiteral getNegation();
	

	public void updateObjectReferences(Set<DomainObject> objects) {
		for (DomainObject obj : objects) {
			if (obj.getName().equals(this.objectName)) {
				this.object = obj;
				break;
			}
		}
	}
	
	public boolean hasUnresolvedReferences() {
		return (this.object == null);
	}

}
