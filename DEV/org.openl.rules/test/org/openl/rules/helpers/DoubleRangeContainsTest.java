package org.openl.rules.helpers;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class DoubleRangeContainsTest {

    @Test
    public void testContains_ValueInsideRange() {
        DoubleRange range = new DoubleRange(15.0, 25.0); // Represents [15.0, 25.0]
        assertFalse(range.contains(Double.NEGATIVE_INFINITY), "Should contain a value in the middle of the range.");
        assertFalse(range.contains(-999999999.99), "Should contain a value in the middle of the range.");
        assertFalse(range.contains(14.999), "Should contain a value in the middle of the range.");
        assertTrue(range.contains(15.0), "Should contain a value in the middle of the range.");
        assertTrue(range.contains(15.001), "Should contain a value in the middle of the range.");
        assertTrue(range.contains(20.0), "Should contain a value in the middle of the range.");
        assertTrue(range.contains(24.999), "Should contain a value in the middle of the range.");
        assertTrue(range.contains(25.0), "Should contain a value in the middle of the range.");
        assertFalse(range.contains(25.001), "Should contain a value in the middle of the range.");
        assertFalse(range.contains(999999999.99), "Should contain a value in the middle of the range.");
        assertFalse(range.contains(Double.POSITIVE_INFINITY), "Should contain a value in the middle of the range.");
        assertFalse(range.contains(Double.NaN), "Should contain a value in the middle of the range.");
        assertFalse(range.contains((Double) null), "Should contain a value in the middle of the range.");
    }

    @Test
    public void testContains_ValueInsideClosedClosedRange() {
        DoubleRange range = new DoubleRange("[15.0; 25.0]");
        assertFalse(range.contains(Double.NEGATIVE_INFINITY), "Should contain a value in the middle of the range.");
        assertFalse(range.contains(-999999999.99), "Should contain a value in the middle of the range.");
        assertFalse(range.contains(14.999), "Should contain a value in the middle of the range.");
        assertTrue(range.contains(15.0), "Should contain a value in the middle of the range.");
        assertTrue(range.contains(15.001), "Should contain a value in the middle of the range.");
        assertTrue(range.contains(20.0), "Should contain a value in the middle of the range.");
        assertTrue(range.contains(24.999), "Should contain a value in the middle of the range.");
        assertTrue(range.contains(25.0), "Should contain a value in the middle of the range.");
        assertFalse(range.contains(25.001), "Should contain a value in the middle of the range.");
        assertFalse(range.contains(999999999.99), "Should contain a value in the middle of the range.");
        assertFalse(range.contains(Double.POSITIVE_INFINITY), "Should contain a value in the middle of the range.");
        assertFalse(range.contains(Double.NaN), "Should contain a value in the middle of the range.");
        assertFalse(range.contains((Double) null), "Should contain a value in the middle of the range.");
    }

    @Test
    public void testContains_ValueInsideClosedOpenedRange() {
        DoubleRange range = new DoubleRange("[15.0; 25.0)");
        assertFalse(range.contains(Double.NEGATIVE_INFINITY), "Should contain a value in the middle of the range.");
        assertFalse(range.contains(-999999999.99), "Should contain a value in the middle of the range.");
        assertFalse(range.contains(14.999), "Should contain a value in the middle of the range.");
        assertTrue(range.contains(15.0), "Should contain a value in the middle of the range.");
        assertTrue(range.contains(15.001), "Should contain a value in the middle of the range.");
        assertTrue(range.contains(20.0), "Should contain a value in the middle of the range.");
        assertTrue(range.contains(24.999), "Should contain a value in the middle of the range.");
        assertFalse(range.contains(25.0), "Should contain a value in the middle of the range.");
        assertFalse(range.contains(25.001), "Should contain a value in the middle of the range.");
        assertFalse(range.contains(999999999.99), "Should contain a value in the middle of the range.");
        assertFalse(range.contains(Double.POSITIVE_INFINITY), "Should contain a value in the middle of the range.");
        assertFalse(range.contains(Double.NaN), "Should contain a value in the middle of the range.");
        assertFalse(range.contains((Double) null), "Should contain a value in the middle of the range.");
    }

    @Test
    public void testContains_ValueInsideOpenedClosedRange() {
        DoubleRange range = new DoubleRange("(15.0; 25.0]");
        assertFalse(range.contains(Double.NEGATIVE_INFINITY), "Should contain a value in the middle of the range.");
        assertFalse(range.contains(-999999999.99), "Should contain a value in the middle of the range.");
        assertFalse(range.contains(14.999), "Should contain a value in the middle of the range.");
        assertFalse(range.contains(15.0), "Should contain a value in the middle of the range.");
        assertTrue(range.contains(15.001), "Should contain a value in the middle of the range.");
        assertTrue(range.contains(20.0), "Should contain a value in the middle of the range.");
        assertTrue(range.contains(24.999), "Should contain a value in the middle of the range.");
        assertTrue(range.contains(25.0), "Should contain a value in the middle of the range.");
        assertFalse(range.contains(25.001), "Should contain a value in the middle of the range.");
        assertFalse(range.contains(999999999.99), "Should contain a value in the middle of the range.");
        assertFalse(range.contains(Double.POSITIVE_INFINITY), "Should contain a value in the middle of the range.");
        assertFalse(range.contains(Double.NaN), "Should contain a value in the middle of the range.");
        assertFalse(range.contains((Double) null), "Should contain a value in the middle of the range.");
    }

    @Test
    public void testContains_ValueInsideOpenedOpenedRange() {
        DoubleRange range = new DoubleRange("(15.0; 25.0)");
        assertFalse(range.contains(Double.NEGATIVE_INFINITY), "Should contain a value in the middle of the range.");
        assertFalse(range.contains(-999999999.99), "Should contain a value in the middle of the range.");
        assertFalse(range.contains(14.999), "Should contain a value in the middle of the range.");
        assertFalse(range.contains(15.0), "Should contain a value in the middle of the range.");
        assertTrue(range.contains(15.001), "Should contain a value in the middle of the range.");
        assertTrue(range.contains(20.0), "Should contain a value in the middle of the range.");
        assertTrue(range.contains(24.999), "Should contain a value in the middle of the range.");
        assertFalse(range.contains(25.0), "Should contain a value in the middle of the range.");
        assertFalse(range.contains(25.001), "Should contain a value in the middle of the range.");
        assertFalse(range.contains(999999999.99), "Should contain a value in the middle of the range.");
        assertFalse(range.contains(Double.POSITIVE_INFINITY), "Should contain a value in the middle of the range.");
        assertFalse(range.contains(Double.NaN), "Should contain a value in the middle of the range.");
        assertFalse(range.contains((Double) null), "Should contain a value in the middle of the range.");
    }

    @Test
    public void testContains_SingleValueRange() {
        DoubleRange range = new DoubleRange(15.0); // Represents [15.0, 15.0]
        assertFalse(range.contains(Double.NEGATIVE_INFINITY), "Should contain a value in the middle of the range.");
        assertFalse(range.contains(-999999999.99), "Should contain a value in the middle of the range.");
        assertFalse(range.contains(14.999), "Should contain a value in the middle of the range.");
        assertTrue(range.contains(15.0), "Should contain a value in the middle of the range.");
        assertFalse(range.contains(15.001), "Should contain a value in the middle of the range.");
        assertFalse(range.contains(999999999.99), "Should contain a value in the middle of the range.");
        assertFalse(range.contains(Double.POSITIVE_INFINITY), "Should contain a value in the middle of the range.");
        assertFalse(range.contains(Double.NaN), "Should contain a value in the middle of the range.");
        assertFalse(range.contains((Double) null), "Should contain a value in the middle of the range.");
    }

    @Test
    public void testContains_WhenRangeIsNaN() {
        // According to DoubleRange implementation, this creates a range where min and max are NaN.
        DoubleRange range = new DoubleRange(Double.NaN, Double.NaN);
        assertFalse(range.contains(15.0), "A NaN range should not contain any valid number.");
        assertFalse(range.contains(Double.NaN), "A NaN range should not contain NaN itself due to NaN comparison rules.");
        assertFalse(range.contains(Double.POSITIVE_INFINITY), "A NaN range should not contain infinity.");
        assertFalse(range.contains(Double.NEGATIVE_INFINITY), "A NaN range should not contain negative infinity.");
    }

    @Test
    public void testContains_RangeToPositiveInfinity() {
        DoubleRange range = new DoubleRange(">=25"); // Represents (25.0..inf)
        assertFalse(range.contains(Double.NEGATIVE_INFINITY), "Should not contain negative infinity.");
        assertFalse(range.contains(-999999999.99), "Should not contain a very small number.");
        assertTrue(range.contains(25.0), "Should contain the lower bound.");
        assertTrue(range.contains(25.001), "Should contain a value just above the boundary.");
        assertTrue(range.contains(999999999.99), "Should contain a very large number.");
        assertTrue(range.contains(Double.POSITIVE_INFINITY), "Should contain positive infinity if the bound is exclusive.");
        assertFalse(range.contains(Double.NaN), "Should not contain the NaN value.");
        assertFalse(range.contains((Double) null), "Should return false for a null input.");
    }

    @Test
    public void testContains_MoreThanRange() {
        DoubleRange range = new DoubleRange("> 25.0"); // Represents (25.0..inf)
        assertFalse(range.contains(Double.NEGATIVE_INFINITY), "Should not contain negative infinity.");
        assertFalse(range.contains(-999999999.99), "Should not contain a very small number.");
        assertFalse(range.contains(25.0), "Should not contain the boundary value.");
        assertTrue(range.contains(25.001), "Should contain a value just above the boundary.");
        assertTrue(range.contains(999999999.99), "Should contain a very large number.");
        assertTrue(range.contains(Double.POSITIVE_INFINITY), "Should contain positive infinity.");
        assertFalse(range.contains(Double.NaN), "Should not contain the NaN value.");
        assertFalse(range.contains((Double) null), "Should return false for a null input.");
    }

    @Test
    public void testContains_LessThanOrEqualRange() {
        DoubleRange range = new DoubleRange("<= 25.0"); // Represents (-inf..25.0]
        assertTrue(range.contains(Double.NEGATIVE_INFINITY), "Should contain negative infinity.");
        assertTrue(range.contains(-999999999.99), "Should contain a very small (large negative) number.");
        assertTrue(range.contains(24.999), "Should not contain a value just above the boundary.");
        assertTrue(range.contains(25.0), "Should contain the boundary value.");
        assertFalse(range.contains(999999999.99), "Should contain a very large number.");
        assertFalse(range.contains(Double.POSITIVE_INFINITY), "Should contain positive infinity.");
        assertFalse(range.contains(Double.NaN), "Should not contain the NaN value.");
        assertFalse(range.contains((Double) null), "Should return false for a null input.");
    }

    @Test
    public void testContains_LessThanRange() {
        DoubleRange range = new DoubleRange("< 25.0"); // Represents (-inf..25.0)
        assertTrue(range.contains(Double.NEGATIVE_INFINITY), "Should contain negative infinity.");
        assertTrue(range.contains(-999999999.99), "Should contain a very small (large negative) number.");
        assertTrue(range.contains(24.999), "Should not contain a value just above the boundary.");
        assertFalse(range.contains(25.0), "Should contain the boundary value.");
        assertFalse(range.contains(999999999.99), "Should contain a very large number.");
        assertFalse(range.contains(Double.POSITIVE_INFINITY), "Should contain positive infinity.");
        assertFalse(range.contains(Double.NaN), "Should not contain the NaN value.");
        assertFalse(range.contains((Double) null), "Should return false for a null input.");
    }
}
