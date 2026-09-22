package org.openl.rules.dt.algorithm.evaluator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FloatTypeComparatorTest {

    private final FloatTypeComparator comparator = FloatTypeComparator.getInstance();

    @Test
    void valuesWithinOneUlpAreEqual() {
        assertEquals(0, comparator.compare(1.0, 1.0));
        assertEquals(0, comparator.compare(1.0f, 1.0));
        assertEquals(0, comparator.compare(0.1 + 0.2, 0.3));
    }

    @Test
    void distinctValuesKeepTheirOrder() {
        assertTrue(comparator.compare(1.0, 2.0) < 0);
        assertTrue(comparator.compare(2.0, 1.0) > 0);
        assertTrue(comparator.compare(1.0, 1.0 + 3 * Math.ulp(1.0)) < 0);
    }
}
