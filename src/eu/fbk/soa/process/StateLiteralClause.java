package eu.fbk.soa.process;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;

import eu.fbk.soa.evolution.sts.Clause;
import eu.fbk.soa.process.domain.DomainObject;
import eu.fbk.soa.process.domain.ObjectLiteral;
import eu.fbk.soa.process.domain.StateLiteral;

@XmlAccessorType(XmlAccessType.NONE)
public class StateLiteralClause implements Clause<StateLiteral> {

	@XmlElements(@XmlElement(name = "literal", namespace = "http://soa.fbk.eu/Object"))
	private Set<StateLiteral> literals = new HashSet<StateLiteral>();

	public StateLiteralClause() {
	}

	public StateLiteralClause(StateLiteral prop) {
		this.addLiteral(prop);
	}

	public StateLiteralClause(StateLiteral...stateProps) {
		for (StateLiteral p : stateProps) {
			this.literals.add(p);
		}
	}
	
	public StateLiteralClause(Set<StateLiteral> lits) {
		this.literals.addAll(lits);
	}


	public void addLiteral(StateLiteral p) {
		literals.add((StateLiteral) p);
	}

	@Override
	public Set<StateLiteral> getLiterals() {
		return literals;
	}

	public boolean isUnitClause() {
		return (literals.size() == 1);
	}

	public StateLiteral getFirstLiteral() {
		if (this.literals.size() >= 1) {
			return this.literals.iterator().next();
		} 
		return null;
	}
	
	public String toString() {
		if (literals.isEmpty()) {
			return "F";
		}
		StringBuffer buffer = new StringBuffer();
		
		Iterator<StateLiteral> litIterator = literals.iterator();
		while (litIterator.hasNext()) {
			StateLiteral lit = litIterator.next();
			buffer.append(lit.toString());
			if (litIterator.hasNext()) {
				buffer.append(" | ");
			}
		}
		
		if (literals.size() > 1) {
			buffer.insert(0, '(').append(')');
		} 
		return buffer.toString();		
	}

	public boolean isRelatedTo(DomainObject obj) {
		for (ObjectLiteral p : this.literals) {
			if (p.isRelatedTo(obj)) {
				return true;
			}
		}
		return false;
	}


	public StateLiteralClause getCopy() {
		StateLiteralClause newClause = new StateLiteralClause();
		for (StateLiteral sl : this.literals) {
			newClause.addLiteral(sl);
		}
		return newClause;
	}

	public void removeLiteral(StateLiteral lit) {
		this.literals.remove(lit); 
	}

	public boolean isEmpty() {
		return (literals.isEmpty());
	}

	public void updateObjectReferences(Set<DomainObject> objects) {
		for (ObjectLiteral l : literals) {
			l.updateObjectReferences(objects);
		}
	}

	public boolean containsLiteral(StateLiteral l) {
		return this.literals.contains(l);
	}

	
	public Set<DomainObject> getRelatedDomainObjects() {
		Set<DomainObject> objects = new HashSet<DomainObject>();
		for (ObjectLiteral lit : literals) {
			DomainObject obj = lit.getDomainObject();
			if (obj != null) {
				objects.add(obj);
			}
		}
		return objects;
	}
	
	public boolean equals(Object obj) {
		if (obj instanceof StateLiteralClause) {
			StateLiteralClause clause = (StateLiteralClause) obj;
			if (this.literals.containsAll(clause.getLiterals()) &&
					clause.getLiterals().containsAll(literals)) {
				return true;
			}
		}
		return false;
	}
	
	public int hashCode() {
		int hashCode = 0;
		for (StateLiteral lit : literals) {
			hashCode += lit.hashCode();
		}
		return hashCode;
	}
	
	public boolean hasUnresolvedReferences() {
		for (ObjectLiteral l : literals) {
			if (l.hasUnresolvedReferences()) {
				return true;
			}
		}
		return false;
	}

	public void addLiterals(Set<StateLiteral> lits) {
		this.literals.addAll(lits);
	}

	public int getNumberOfLiterals() {
		return this.literals.size();
	}
}
