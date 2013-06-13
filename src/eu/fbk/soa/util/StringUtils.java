package eu.fbk.soa.util;

import java.util.Collection;
import java.util.Iterator;

public final class StringUtils {

	private StringUtils() { }

	public static String getCommaSeparatedString(Collection<? extends Object> collection) {
		StringBuffer buffer = new StringBuffer();		

		Iterator<? extends Object> iterator = collection.iterator();
		while (iterator.hasNext()) {
			Object obj = iterator.next();
			buffer.append(obj.toString());

			if (iterator.hasNext()) {
				buffer.append(", ");
			}
		}
		return buffer.toString();
	}
}
