package org.openl.rules.testmethod.test;

import static org.junit.Assert.*;

import org.junit.Test;
import org.openl.rules.testmethod.result.ComparableResultComparator;

public class ComparableResultComparatorTest {
    @Test
    public void test() {
        ComparableResultComparator comp = new ComparableResultComparator();

        assertTrue(comp.compareResult(null, null, null));

        assertFalse(comp.compareResult(null, Integer.valueOf(10), null));

        assertFalse(comp.compareResult(Integer.valueOf(10), null, null));

        assertTrue(comp.compareResult(Integer.valueOf(10), Integer.valueOf(10), null));
    }
}
