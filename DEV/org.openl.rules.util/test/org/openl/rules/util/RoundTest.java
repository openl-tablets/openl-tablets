package org.openl.rules.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class RoundTest {

    @Test
    void testRoundDouble() {

        assertNull(Round.round((Double) null));
        assertEquals(0, Round.round(Double.NaN));
        assertEquals(Integer.MAX_VALUE, Round.round(Double.POSITIVE_INFINITY));
        assertEquals(Integer.MIN_VALUE, Round.round(Double.NEGATIVE_INFINITY));

        assertEquals(0, Round.round(0d));
        assertEquals(0, Round.round(Math.ulp(0d)));
        assertEquals(0, Round.round(-Math.ulp(0d)));

        assertEquals(1, Round.round(0.5));
        assertEquals(1, Round.round(0.49999999999999992d));
        assertEquals(0, Round.round(0.49999999999999991d));

        assertEquals(2, Round.round(1.5));
        assertEquals(2, Round.round(1.4999999999999998d));
        assertEquals(1, Round.round(1.4999999999999996d));

        assertEquals(-1, Round.round(-0.5));
        assertEquals(-1, Round.round(-0.49999999999999992d));
        assertEquals(-0, Round.round(-0.49999999999999991d));

        assertEquals(-2, Round.round(-1.5));
        assertEquals(-2, Round.round(-1.4999999999999998d));
        assertEquals(-1, Round.round(-1.4999999999999996d));
    }

    @Test
    void testRoundDoubleBeyondIntegerRange() {
        assertEquals(2147483647, Round.round(2147483646.5));
        assertEquals(-2147483648, Round.round(-2147483647.5));

        // the limits of the Integer range
        assertEquals(Integer.MAX_VALUE, Round.round(2147483647.5));
        assertEquals(Integer.MIN_VALUE, Round.round(-2147483648.5));
        assertEquals(Integer.MAX_VALUE, Round.round(5000000000.4));
        assertEquals(Integer.MIN_VALUE, Round.round(-5000000000.4));
        assertEquals(Integer.MAX_VALUE, Round.round(1e19));
    }

    @Test
    void testRoundDoubleWithMode() {
        assertNull(Round.round((Double) null, Round.DOWN));

        assertEquals(2, Round.round(2.5, Round.HALF_EVEN));
        assertEquals(2, Round.round(2.5, Round.HALF_DOWN));
        assertEquals(-3, Round.round(-2.5, Round.HALF_UP));
        assertEquals(3, Round.round(3.0, Round.UNNECESSARY));
        assertThrows(ArithmeticException.class, () -> Round.round(2.5, Round.UNNECESSARY));

        // the limits of the Integer range in every rounding mode
        assertEquals(2147483647, Round.round(2147483647.5, Round.DOWN));
        assertEquals(-2147483648, Round.round(-2147483647.5, Round.UP));
        assertEquals(Integer.MAX_VALUE, Round.round(2147483647.5, Round.UP));
        assertEquals(Integer.MIN_VALUE, Round.round(-2147483648.5, Round.UP));
        assertEquals(Integer.MAX_VALUE, Round.round(5000000000.4, Round.DOWN));
        assertEquals(Integer.MIN_VALUE, Round.round(-5000000000.4, Round.DOWN));
        assertEquals(Integer.MAX_VALUE, Round.round(1e19, Round.DOWN));

        assertEquals(0, Round.round(Double.NaN, Round.UP));
        assertEquals(Integer.MAX_VALUE, Round.round(Double.POSITIVE_INFINITY, Round.DOWN));
        assertEquals(Integer.MIN_VALUE, Round.round(Double.NEGATIVE_INFINITY, Round.UP));
    }

    @Test
    void testRoundFloat() {

        assertNull(Round.round((Float) null));
        assertEquals(0, Round.round(Float.NaN));
        assertEquals(Integer.MAX_VALUE, Round.round(Float.POSITIVE_INFINITY));
        assertEquals(Integer.MIN_VALUE, Round.round(Float.NEGATIVE_INFINITY));

        assertEquals(0, Round.round(0f));
        assertEquals(0, Round.round(Math.ulp(0f)));
        assertEquals(0, Round.round(-Math.ulp(0f)));

        assertEquals(1, Round.round(0.5f));
        assertEquals(1, Round.round(0.49999999f));
        assertEquals(0, Round.round(0.49999998f));

        assertEquals(2, Round.round(1.5f));
        assertEquals(2, Round.round(1.49999995f));
        assertEquals(1, Round.round(1.49999994f));

        assertEquals(-1, Round.round(-0.5f));
        assertEquals(-1, Round.round(-0.49999999f));
        assertEquals(-0, Round.round(-0.49999998f));

        assertEquals(-2, Round.round(-1.5f));
        assertEquals(-2, Round.round(-1.49999995f));
        assertEquals(-1, Round.round(-1.49999994f));

        // the largest float below 2^31, then the limits of the Integer range
        assertEquals(2147483520, Round.round(2147483520f));
        assertEquals(Integer.MAX_VALUE, Round.round(5.0E9f));
        assertEquals(Integer.MIN_VALUE, Round.round(-5.0E9f));
        assertEquals(Integer.MIN_VALUE, Round.round(-2147483648f));
    }

    @Test
    void testRoundFloatWithMode() {
        assertNull(Round.round((Float) null, Round.DOWN));

        assertEquals(2, Round.round(2.5f, Round.HALF_EVEN));
        assertEquals(3, Round.round(2.5f, Round.HALF_UP));
        assertEquals(-2, Round.round(-2.7f, Round.DOWN));

        assertEquals(0, Round.round(Float.NaN, Round.UP));
        assertEquals(Integer.MAX_VALUE, Round.round(Float.POSITIVE_INFINITY, Round.FLOOR));
        assertEquals(Integer.MAX_VALUE, Round.round(5.0E9f, Round.DOWN));
        assertEquals(Integer.MIN_VALUE, Round.round(-5.0E9f, Round.UP));
        assertEquals(Integer.MIN_VALUE, Round.round(-2147483648f, Round.CEILING));
    }

    @Test
    void testRoundFloatPlaces() {
        assertNull(Round.round((Float) null, 2));
        assertNull(Round.round((Float) null, 2, Round.DOWN));
        assertEquals(32.29f, Round.round(32.285f, 2));
        assertEquals(32.28f, Round.round(32.285f, 2, Round.DOWN));
        assertEquals(32.28f, Round.round(32.285f, 2, 1));
    }

    @Test
    void testRoundWholeNumber() {
        assertNull(Round.round((Long) null));
        assertNull(Round.round((Long) null, Round.DOWN));
        assertNull(Round.round((Long) null, 2));
        assertNull(Round.round((Long) null, -2, Round.DOWN));
        assertNull(Round.round((Long) null, -2, 1));

        // 2^24 + 1, the first whole number a Float cannot hold
        assertEquals(16777217, Round.round(16777217L));
        assertEquals(16777217, Round.round(16777217L, Round.DOWN));
        assertEquals(16777217, Round.round(16777217L, Round.UNNECESSARY));
        assertEquals(Integer.MAX_VALUE, Round.round(5000000000L));
        assertEquals(Integer.MIN_VALUE, Round.round(-5000000000L, Round.UP));

        // decimal places give a Double, as for a Double
        assertEquals(16777217.0, Round.round(16777217L, 0));
        assertEquals(16777217.0, Round.round(16777217L, 2));
        assertEquals(5.0E9, Round.round(5000000000L, 0));

        assertEquals(12300.0, Round.round(12345L, -2));
        assertEquals(12400.0, Round.round(12350L, -2));
        assertEquals(-12400.0, Round.round(-12350L, -2));
        assertEquals(12200.0, Round.round(12250L, -2, Round.HALF_EVEN));
        assertEquals(12300.0, Round.round(12399L, -2, Round.DOWN));
        assertEquals(12400.0, Round.round(12301L, -2, 0));
        assertThrows(ArithmeticException.class, () -> Round.round(12345L, -2, Round.UNNECESSARY));
    }

    @Test
    void testRoundStrict() {

        assertNull(Round.roundStrict(null));

        assertEquals(toLong(0L), Round.roundStrict(Double.NaN));
        assertEquals(toLong(Long.MAX_VALUE), Round.roundStrict(Double.POSITIVE_INFINITY));
        assertEquals(toLong(Long.MIN_VALUE), Round.roundStrict(Double.NEGATIVE_INFINITY));

        assertEquals(toLong(0L), Round.roundStrict(0d));
        assertEquals(toLong(0L), Round.roundStrict(Math.ulp(0d)));
        assertEquals(toLong(0L), Round.roundStrict(-Math.ulp(0d)));

        assertEquals(toLong(1L), Round.roundStrict(0.5));
        assertEquals(toLong(0L), Round.roundStrict(0.49999999999999992d));
        assertEquals(toLong(0L), Round.roundStrict(0.49999999999999991d));

        assertEquals(toLong(2L), Round.roundStrict(1.5));
        assertEquals(toLong(1L), Round.roundStrict(1.4999999999999998d));
        assertEquals(toLong(1L), Round.roundStrict(1.4999999999999996d));

        assertEquals(toLong(-1L), Round.roundStrict(-0.5));
        assertEquals(toLong(0L), Round.roundStrict(-0.49999999999999992d));
        assertEquals(toLong(0L), Round.roundStrict(-0.49999999999999991d));

        assertEquals(toLong(-2L), Round.roundStrict(-1.5));
        assertEquals(toLong(-1L), Round.roundStrict(-1.4999999999999998d));
        assertEquals(toLong(-1L), Round.roundStrict(-1.4999999999999996d));
    }

    private static Long toLong(long v) {
        return v;
    }

    @Test
    void testRound2() {
        assertEquals("1.222", String.valueOf(Round.round(1.222235345345, 3)));
        assertEquals("1.6", String.valueOf(Round.round(1.56000001235345345, 1)));
        assertEquals("0.0", String.valueOf(Round.round(0.0, 0)));
        assertNull(Round.round((Double) null, 2));

        assertEquals(2.68, Round.round(2.675, 2));
        assertEquals(-2.68, Round.round(-2.675, 2));
        assertEquals(32.28, Round.round(32.285, 2, Round.DOWN));
        assertEquals(32.28, Round.round(32.285, 2, 1));
        assertNull(Round.round((Double) null, 2, Round.DOWN));

        // a whole number keeps its last digit
        assertEquals(1.0E15, Round.round(1.0E15, 1));
        assertEquals(2251799813685249.0, Round.round(2251799813685249.0, 0));
        assertEquals(4503599627370497.0, Round.round(4503599627370497.0, 0));
        assertEquals(4503599627370490.0, Round.round(4503599627370494.0, -1));
    }
}
