package org.openl.rules.testmethod.result;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class GenericComparatorTest {
    @Test
    public void test() {
        GenericComparator comparator = new GenericComparator();
        assertTrue(comparator.isEqual(null, null));
        assertTrue(comparator.isEqual(10, 10));
        assertFalse(comparator.isEqual(null, "hello"));
        assertFalse(comparator.isEqual("no hello", null));
    }
}
