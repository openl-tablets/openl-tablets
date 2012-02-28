package org.openl.rules.testmethod.test;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openl.rules.testmethod.result.DefaultComparator;

public class DefaultComparatorTest {
	@Test
	public void test() {
		DefaultComparator comparator = new DefaultComparator();
		assertTrue(comparator.compareResult(null, null));
		assertTrue(comparator.compareResult(Integer.valueOf(10), Integer.valueOf(10)));
		assertFalse(comparator.compareResult("hello", null));
		assertFalse(comparator.compareResult(null, "no hello"));
	}
}
