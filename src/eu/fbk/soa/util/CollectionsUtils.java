package eu.fbk.soa.util;

import java.util.Collection;

public class CollectionsUtils {

	public static boolean nonEmptyIntersection(Collection<? extends Object> collection1,
			Collection<? extends Object> collection2) {
		
		for (Object obj : collection1) {
			if (collection2.contains(obj)) {
				return true;
			}
		}
		return false;
	}

	
}
