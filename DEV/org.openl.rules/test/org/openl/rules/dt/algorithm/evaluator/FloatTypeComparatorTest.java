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
        assertEquals(0, comparator.compare(-0.0, 0.0));
    }

    @Test
    void distinctValuesKeepTheirOrder() {
        assertTrue(comparator.compare(1.0, 2.0) < 0);
        assertTrue(comparator.compare(2.0, 1.0) > 0);
        assertTrue(comparator.compare(1.0, 1.0 + 3 * Math.ulp(1.0)) < 0);
    }

    @Test
    void nanEqualsOnlyNaN() {
        assertEquals(0, comparator.compare(Double.NaN, Double.NaN));
        assertEquals(0, comparator.compare(Float.NaN, Double.NaN));
        assertTrue(comparator.compare(Double.NaN, 0.0) > 0);
        assertTrue(comparator.compare(0.0, Double.NaN) < 0);
        assertTrue(comparator.compare(Double.NaN, Double.POSITIVE_INFINITY) > 0);
        assertTrue(comparator.compare(Double.POSITIVE_INFINITY, Double.NaN) < 0);
    }

    @Test
    void infinityEqualsOnlyTheInfinityOfTheSameSign() {
        assertEquals(0, comparator.compare(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY));
        assertEquals(0, comparator.compare(Float.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY));
        assertTrue(comparator.compare(Double.POSITIVE_INFINITY, Double.MAX_VALUE) > 0);
        assertTrue(comparator.compare(Double.MAX_VALUE, Double.POSITIVE_INFINITY) < 0);
        assertTrue(comparator.compare(Double.NEGATIVE_INFINITY, -Double.MAX_VALUE) < 0);
        assertTrue(comparator.compare(-Double.MAX_VALUE, Double.NEGATIVE_INFINITY) > 0);
        assertTrue(comparator.compare(Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY) < 0);
        assertTrue(comparator.compare(Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY) > 0);
    }
}
