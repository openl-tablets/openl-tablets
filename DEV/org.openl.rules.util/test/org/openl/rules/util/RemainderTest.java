package org.openl.rules.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;

class RemainderTest {

    @Test
    void remainderInt() {
        // Truncated remainder: same sign as dividend.
        assertEquals(1, Remainder.remainder(7, 3));
        assertEquals(-1, Remainder.remainder(-7, 3));
        assertEquals(1, Remainder.remainder(7, -3));
        assertEquals(-1, Remainder.remainder(-7, -3));
        assertEquals(6, Remainder.remainder(19, 13));
        assertEquals(6, Remainder.remainder(19, -13));
        assertEquals(-6, Remainder.remainder(-19, 13));
        assertEquals(-6, Remainder.remainder(-19, -13));
        assertEquals(0, Remainder.remainder(19, 19));
        assertEquals(0, Remainder.remainder(0, 19));
    }

    @Test
    void remainderQuotientIdentity() {
        // a == n * quotient(a, n) + remainder(a, n)
        int[][] pairs = {{19, 5}, {-19, 5}, {19, -5}, {-19, -5}, {7, 3}, {-10, 3}, {10, -3}};
        for (int[] p : pairs) {
            int a = p[0], n = p[1];
            assertEquals(a, n * Quotient.quotient(a, n) + Remainder.remainder(a, n));
        }
    }

    @Test
    void remainderIntByZero() {
        assertThrows(ArithmeticException.class, () -> Remainder.remainder(5, 0));
    }

    @Test
    void remainderIntOverflowEdgeCase() {
        // Integer.MIN_VALUE % -1 returns 0 in Java.
        assertEquals(0, Remainder.remainder(Integer.MIN_VALUE, -1));
    }

    @Test
    void remainderLong() {
        assertEquals(6L, Remainder.remainder(19L, 13L));
        assertEquals(-6L, Remainder.remainder(-19L, 13L));
        assertEquals(0L, Remainder.remainder(Long.MIN_VALUE, -1L));
    }

    @Test
    void remainderLongByZero() {
        assertThrows(ArithmeticException.class, () -> Remainder.remainder(5L, 0L));
    }

    @Test
    void remainderByte() {
        assertEquals((byte) 6, Remainder.remainder((byte) 19, (byte) 13));
        assertEquals((byte) 6, Remainder.remainder((byte) 19, (byte) -13));
        assertEquals((byte) -6, Remainder.remainder((byte) -19, (byte) 13));
        assertEquals((byte) -6, Remainder.remainder((byte) -19, (byte) -13));
        assertEquals((byte) 1, Remainder.remainder(Byte.MAX_VALUE, (byte) 7));
        assertEquals((byte) -2, Remainder.remainder(Byte.MIN_VALUE, (byte) 7));
    }

    @Test
    void remainderShort() {
        assertEquals((short) 6, Remainder.remainder((short) 19, (short) 13));
        assertEquals((short) -6, Remainder.remainder((short) -19, (short) 13));
        assertEquals((short) 9, Remainder.remainder(Short.MAX_VALUE, (short) 11));
    }

    @Test
    void remainderFloat() {
        assertEquals(1.47f, Remainder.remainder(3.22f, 1.75f), 0.001f);
        assertEquals(1.47f, Remainder.remainder(3.22f, -1.75f), 0.001f);
        assertEquals(-1.47f, Remainder.remainder(-3.22f, 1.75f), 0.001f);
        assertEquals(-1.47f, Remainder.remainder(-3.22f, -1.75f), 0.001f);
        assertEquals(0.0f, Remainder.remainder(3.5f, 1.75f), 0.0f);
        assertEquals(0.0f, Remainder.remainder(0.0f, 1.75f), 0.0f);
    }

    @Test
    void remainderFloatByZero() {
        // x rem 0.0f == NaN (IEEE 754)
        assertTrue(Float.isNaN(Remainder.remainder(3.22f, 0.0f)));
        assertTrue(Float.isNaN(Remainder.remainder(-3.22f, 0.0f)));
        assertTrue(Float.isNaN(Remainder.remainder(0.0f, 0.0f)));
    }

    @Test
    void remainderFloatNanAndInfinity() {
        // NaN propagates via Java's %.
        assertTrue(Float.isNaN(Remainder.remainder(Float.NaN, 3.0f)));
        assertTrue(Float.isNaN(Remainder.remainder(3.0f, Float.NaN)));
        // finite % ±Inf = finite (matches IEEE 754).
        assertEquals(3.0f, Remainder.remainder(3.0f, Float.POSITIVE_INFINITY), 0.0f);
        assertEquals(-3.0f, Remainder.remainder(-3.0f, Float.POSITIVE_INFINITY), 0.0f);
        // ±Inf % finite = NaN.
        assertTrue(Float.isNaN(Remainder.remainder(Float.POSITIVE_INFINITY, 3.0f)));
        assertTrue(Float.isNaN(Remainder.remainder(Float.NEGATIVE_INFINITY, 3.0f)));
    }

    @Test
    void remainderDouble() {
        assertEquals(1.47, Remainder.remainder(3.22, 1.75), 0.001);
        assertEquals(1.47, Remainder.remainder(3.22, -1.75), 0.001);
        assertEquals(-1.47, Remainder.remainder(-3.22, 1.75), 0.001);
        assertEquals(-1.47, Remainder.remainder(-3.22, -1.75), 0.001);
        assertEquals(0.0, Remainder.remainder(0.0, 1.75), 0.0);
    }

    @Test
    void remainderDoubleByZero() {
        assertTrue(Double.isNaN(Remainder.remainder(3.22, 0.0)));
        assertTrue(Double.isNaN(Remainder.remainder(-3.22, 0.0)));
        assertTrue(Double.isNaN(Remainder.remainder(0.0, 0.0)));
    }

    @Test
    void remainderDoubleNanAndInfinity() {
        assertTrue(Double.isNaN(Remainder.remainder(Double.NaN, 3.0)));
        assertTrue(Double.isNaN(Remainder.remainder(3.0, Double.NaN)));
        assertEquals(3.0, Remainder.remainder(3.0, Double.POSITIVE_INFINITY), 0.0);
        assertEquals(-3.0, Remainder.remainder(-3.0, Double.POSITIVE_INFINITY), 0.0);
        assertTrue(Double.isNaN(Remainder.remainder(Double.POSITIVE_INFINITY, 3.0)));
        assertTrue(Double.isNaN(Remainder.remainder(Double.NEGATIVE_INFINITY, 3.0)));
    }

    @Test
    void remainderBigInteger() {
        assertEquals(BigInteger.valueOf(6), Remainder.remainder(BigInteger.valueOf(19), BigInteger.valueOf(13)));
        assertEquals(BigInteger.valueOf(6), Remainder.remainder(BigInteger.valueOf(19), BigInteger.valueOf(-13)));
        assertEquals(BigInteger.valueOf(-6), Remainder.remainder(BigInteger.valueOf(-19), BigInteger.valueOf(13)));
        assertEquals(BigInteger.valueOf(-6), Remainder.remainder(BigInteger.valueOf(-19), BigInteger.valueOf(-13)));
        assertEquals(BigInteger.ZERO, Remainder.remainder(BigInteger.valueOf(19), BigInteger.valueOf(19)));
        assertEquals(BigInteger.ZERO, Remainder.remainder(BigInteger.ZERO, BigInteger.valueOf(19)));
    }

    @Test
    void remainderBigIntegerLarge() {
        BigInteger dividend = new BigInteger("203000745502000030060144252100");
        assertEquals(BigInteger.valueOf(59), Remainder.remainder(dividend, BigInteger.valueOf(97)));
        assertEquals(BigInteger.valueOf(59), Remainder.remainder(dividend, BigInteger.valueOf(-97)));
        assertEquals(BigInteger.valueOf(-59), Remainder.remainder(dividend.negate(), BigInteger.valueOf(97)));
        assertEquals(BigInteger.valueOf(-59), Remainder.remainder(dividend.negate(), BigInteger.valueOf(-97)));
    }

    @Test
    void remainderBigIntegerByZero() {
        var dividend = BigInteger.valueOf(19);
        assertThrows(ArithmeticException.class, () -> Remainder.remainder(dividend, BigInteger.ZERO));
    }

    @Test
    void remainderBigDecimal() {
        assertEquals(BigDecimal.valueOf(1.47), Remainder.remainder(BigDecimal.valueOf(3.22), BigDecimal.valueOf(1.75)));
        assertEquals(BigDecimal.valueOf(1.47), Remainder.remainder(BigDecimal.valueOf(3.22), BigDecimal.valueOf(-1.75)));
        assertEquals(BigDecimal.valueOf(-1.47), Remainder.remainder(BigDecimal.valueOf(-3.22), BigDecimal.valueOf(1.75)));
        assertEquals(BigDecimal.valueOf(-1.47), Remainder.remainder(BigDecimal.valueOf(-3.22), BigDecimal.valueOf(-1.75)));
    }

    @Test
    void remainderBigDecimalLarge() {
        BigDecimal dividend = new BigDecimal("203000745502000030060144252100");
        assertEquals(BigDecimal.valueOf(59), Remainder.remainder(dividend, BigDecimal.valueOf(97)));
    }

    @Test
    void remainderBigDecimalByZero() {
        var dividend = BigDecimal.valueOf(19);
        assertThrows(ArithmeticException.class, () -> Remainder.remainder(dividend, BigDecimal.ZERO));
    }

    @Test
    void remainderBoxedNulls() {
        assertNull(Remainder.remainder(null, (byte) 13));
        assertNull(Remainder.remainder((byte) 13, null));

        assertNull(Remainder.remainder(null, (short) 13));
        assertNull(Remainder.remainder((short) 13, null));

        assertNull(Remainder.remainder(null, 13));
        assertNull(Remainder.remainder(13, null));

        assertNull(Remainder.remainder(null, 13L));
        assertNull(Remainder.remainder(13L, null));

        assertNull(Remainder.remainder(null, 1.75f));
        assertNull(Remainder.remainder(1.75f, null));

        assertNull(Remainder.remainder(null, 1.75));
        assertNull(Remainder.remainder(1.75, null));

        assertNull(Remainder.remainder(null, BigInteger.valueOf(13)));
        assertNull(Remainder.remainder(BigInteger.valueOf(13), null));

        assertNull(Remainder.remainder(null, BigDecimal.valueOf(13)));
        assertNull(Remainder.remainder(BigDecimal.valueOf(13), null));
    }

    @Test
    void remainderBoxedHappyPath() {
        assertEquals((byte) 6, Remainder.remainder((byte) 19, (byte) 13));
        assertEquals((short) 6, Remainder.remainder((short) 19, (short) 13));
        assertEquals(1, Remainder.remainder(7, 3));
        assertEquals(1L, Remainder.remainder(7L, 3L));
        assertEquals(1.47f, Remainder.remainder(3.22f, 1.75f), 0.001f);
        assertEquals(1.47, Remainder.remainder(3.22, 1.75), 0.001);
    }
}
