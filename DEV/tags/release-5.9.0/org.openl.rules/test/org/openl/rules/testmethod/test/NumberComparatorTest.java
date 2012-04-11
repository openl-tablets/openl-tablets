package org.openl.rules.testmethod.test;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openl.rules.testmethod.result.NumberComparator;

public class NumberComparatorTest {
	@Test
	public void test() {
		NumberComparator comp = new NumberComparator();
		
		assertTrue(comp.compareResult(null, null));
		
		Double value = Double.valueOf(10);
		
		assertFalse(comp.compareResult(null, value));
		
		assertFalse(comp.compareResult(value, null));
		
		assertTrue(comp.compareResult(value, value));
	}
}
