package org.openl.rules.dt.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import org.openl.rules.helpers.DoubleRange;

class DoubleRangeAdaptorTest {

    @Test
    void testMax() {
        IRangeAdaptor<DoubleRange, Double> adaptor = DoubleRangeAdaptor.getInstance();

        var range = new DoubleRange("[1;15]");
        assertEquals(15, adaptor.getMax(range), Math.ulp(15));

        var range1 = new DoubleRange("[1;15)");
        assertEquals(15, adaptor.getMax(range1), Math.ulp(15));
    }

    @Test
    void rangeWithoutUpperLimitEndsAboveInfinity() {
        var adaptor = DoubleRangeAdaptor.getInstance();

        assertEquals(Double.NaN, adaptor.getMax(new DoubleRange("> 10")));
        assertEquals(Double.NaN, adaptor.getMax(new DoubleRange(">= 10")));
        assertEquals(Double.NaN, adaptor.getMax(new DoubleRange("10+")));
        assertEquals(Double.NaN, adaptor.getMax(new DoubleRange(10, Double.POSITIVE_INFINITY)));
        assertTrue(Double.compare(Double.POSITIVE_INFINITY, adaptor.getMax(new DoubleRange("> 10"))) < 0);
    }

    @Test
    void rangeWithoutLowerLimitStartsAtMinusInfinity() {
        var adaptor = DoubleRangeAdaptor.getInstance();

        assertEquals(Double.NEGATIVE_INFINITY, adaptor.getMin(new DoubleRange("< 0")));
        assertEquals(Double.NEGATIVE_INFINITY, adaptor.getMin(new DoubleRange(Double.NEGATIVE_INFINITY, 0)));
    }

    @Test
    void boundsMoveToTheNextNumber() {
        var adaptor = DoubleRangeAdaptor.getInstance();

        assertEquals(Math.nextUp(15.0), adaptor.getMax(new DoubleRange("[1;15]")));
        assertEquals(15.0, adaptor.getMax(new DoubleRange("[1;15)")));
        assertEquals(1.0, adaptor.getMin(new DoubleRange("[1;15]")));
        assertEquals(Math.nextUp(1.0), adaptor.getMin(new DoubleRange("(1;15]")));
    }

    @Test
    void negativeBoundsMoveToTheNextNumber() {
        var adaptor = DoubleRangeAdaptor.getInstance();

        assertEquals(-0.9999999999999999, adaptor.getMax(new DoubleRange("[-2; -1]")));
        assertEquals(-0.9999999999999999, adaptor.getMin(new DoubleRange("(-1; 0)")));
        assertEquals(-3.9999999999999996, adaptor.getMax(new DoubleRange("-4")));
    }
}
