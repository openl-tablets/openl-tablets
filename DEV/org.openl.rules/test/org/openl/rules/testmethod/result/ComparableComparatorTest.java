package org.openl.rules.testmethod.result;

import static org.junit.Assert.*;

import org.junit.Test;

public class ComparableComparatorTest {
    @Test
    public void test() {
        ComparableComparator comp = (ComparableComparator) ComparableComparator.getInstance();

        assertTrue(comp.isEqual(null, null));

        assertFalse(comp.isEqual(Integer.valueOf(10), null));

        assertFalse(comp.isEqual(null, Integer.valueOf(10)));

        assertTrue(comp.isEqual(Integer.valueOf(10), Integer.valueOf(10)));
    }
}
