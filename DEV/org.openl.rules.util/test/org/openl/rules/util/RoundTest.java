package org.openl.rules.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class RoundTest {

    @Test
    public void testRoundDouble() {

        assertEquals(0, Round.round(Double.NaN));
        assertEquals(Long.MAX_VALUE, Round.round(Double.POSITIVE_INFINITY));
        assertEquals(Long.MIN_VALUE, Round.round(Double.NEGATIVE_INFINITY));

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
    public void testRoundFloat() {

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
    }

    @Test
    public void testRoundStrict() {

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
    public void testRound2() {
        assertEquals("1.222", String.valueOf(Round.round(1.222235345345, 3)));
        assertEquals("1.6", String.valueOf(Round.round(1.56000001235345345, 1)));
        assertEquals("0.0", String.valueOf(Round.round(0, 0)));
    }
}
