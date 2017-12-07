package org.openl.rules.testmethod.result;

import static org.junit.Assert.*;

import org.junit.Test;

public class GenericComparatorTest {
    @Test
    public void test() {
        GenericComparator comparator = new GenericComparator();
        assertTrue(comparator.compareResult(null, null, null));
        assertTrue(comparator.compareResult(Integer.valueOf(10), Integer.valueOf(10), null));
        assertFalse(comparator.compareResult("hello", null, null));
        assertFalse(comparator.compareResult(null, "no hello", null));
    }
}
