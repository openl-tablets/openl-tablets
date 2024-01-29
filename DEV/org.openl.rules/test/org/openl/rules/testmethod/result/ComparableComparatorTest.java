package org.openl.rules.testmethod.result;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ComparableComparatorTest {
    @Test
    public void test() {
        ComparableComparator comp = (ComparableComparator) ComparableComparator.getInstance();

        assertTrue(comp.isEqual(null, null));

        assertFalse(comp.isEqual(10, null));

        assertFalse(comp.isEqual(null, 10));

        assertTrue(comp.isEqual(10, 10));
    }
}
