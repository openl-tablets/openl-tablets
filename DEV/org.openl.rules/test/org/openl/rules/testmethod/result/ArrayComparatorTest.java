package org.openl.rules.testmethod.result;

import static org.junit.Assert.*;

import org.junit.Test;

public class ArrayComparatorTest {
    @Test
    public void test() {
        ArrayComparator comparator = new ArrayComparator(Integer.class);

        assertTrue(comparator.compareResult(null, null, null));
        Integer[] intArray = new Integer[] {1, 2};
        assertFalse(comparator.compareResult(null, intArray, null));

        assertFalse(comparator.compareResult(intArray, null, null));

        assertTrue(comparator.compareResult(intArray, intArray, null));
    }
}
