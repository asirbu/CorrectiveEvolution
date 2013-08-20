package eu.fbk.soa.util.test;

import static org.junit.Assert.assertEquals;

import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import eu.fbk.soa.util.StringUtils;

public class StringUtilsTest {

	@Test
	public void testEmptyInput() {
		Set<Object> mySet = new HashSet<Object>();
		String commaSeparatedString = StringUtils.getCommaSeparatedString(mySet);
		assertEquals("", commaSeparatedString);
	}
	
	@Test
	public void testIntegerSet() {
		Set<Integer> intSet = new HashSet<Integer>();
		for (int i = 1; i <= 3; i++) {
			intSet.add(new Integer(i));
		}
		String commaSeparatedString = StringUtils.getCommaSeparatedString(intSet);
		assertEquals("1, 2, 3", commaSeparatedString);
	}

}
