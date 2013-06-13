package eu.fbk.soa.process;

import java.util.Set;

import eu.fbk.soa.process.domain.DomainObject;

public interface Goal {

	void updateObjectReferences(Set<DomainObject> objects);

}
