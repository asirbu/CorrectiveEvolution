package eu.fbk.soa.util.test;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import eu.fbk.soa.process.Activity;
import eu.fbk.soa.process.Effect;
import eu.fbk.soa.process.StateFormula;
import eu.fbk.soa.util.ArrayUtils;

public class ArrayUtilsTest {
	
	private List<Activity> activities;
	
	@Test
	public void testPermutations() {
		this.activities = new ArrayList<Activity>();
		
		int n = 5;
		
		int nFactorial = 1;
		for (int i = n; i >= 1; i--) {
			nFactorial *= i;
		}
		
		for (int i = 0; i < n; i++) {
			Activity ai = new Activity("A" + i, new StateFormula(), new Effect());
			activities.add(ai);
		}
		ArrayUtils<Activity> utils = new ArrayUtils<Activity>();
		List<List<Activity>> permutations = utils.computePermutations(activities);
		
		assertEquals("Number of permutations should be equal to n factorial", 
				nFactorial, permutations.size());
		
	}

}
