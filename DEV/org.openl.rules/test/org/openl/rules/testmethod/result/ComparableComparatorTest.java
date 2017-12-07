package org.openl.rules.testmethod.result;

import static org.junit.Assert.*;

import org.junit.Test;

public class ComparableComparatorTest {
    @Test
    public void test() {
        ComparableComparator comp = new ComparableComparator();

        assertTrue(comp.compareResult(null, null, null));

        assertFalse(comp.compareResult(null, Integer.valueOf(10), null));

        assertFalse(comp.compareResult(Integer.valueOf(10), null, null));

        assertTrue(comp.compareResult(Integer.valueOf(10), Integer.valueOf(10), null));
    }
}
