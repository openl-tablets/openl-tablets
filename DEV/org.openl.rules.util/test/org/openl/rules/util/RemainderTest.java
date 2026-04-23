package org.openl.rules.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;

public class RemainderTest {

    @Test
    public void remainderInt() {
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
    public void remainderQuotientIdentity() {
        // a == n * quotient(a, n) + remainder(a, n)
        int[][] pairs = {{19, 5}, {-19, 5}, {19, -5}, {-19, -5}, {7, 3}, {-10, 3}, {10, -3}};
        for (int[] p : pairs) {
            int a = p[0], n = p[1];
            assertEquals(a, n * Quotient.quotient(a, n) + Remainder.remainder(a, n));
        }
    }

    @Test
    public void remainderIntByZero() {
        assertThrows(ArithmeticException.class, () -> Remainder.remainder(5, 0));
    }

    @Test
    public void remainderIntOverflowEdgeCase() {
        // Integer.MIN_VALUE % -1 returns 0 in Java.
        assertEquals(0, Remainder.remainder(Integer.MIN_VALUE, -1));
    }

    @Test
    public void remainderLong() {
        assertEquals(6L, Remainder.remainder(19L, 13L));
        assertEquals(-6L, Remainder.remainder(-19L, 13L));
        assertEquals(0L, Remainder.remainder(Long.MIN_VALUE, -1L));
    }

    @Test
    public void remainderLongByZero() {
        assertThrows(ArithmeticException.class, () -> Remainder.remainder(5L, 0L));
    }

    @Test
    public void remainderByte() {
        assertEquals((byte) 6, Remainder.remainder((byte) 19, (byte) 13));
        assertEquals((byte) 6, Remainder.remainder((byte) 19, (byte) -13));
        assertEquals((byte) -6, Remainder.remainder((byte) -19, (byte) 13));
        assertEquals((byte) -6, Remainder.remainder((byte) -19, (byte) -13));
        assertEquals((byte) 1, Remainder.remainder(Byte.MAX_VALUE, (byte) 7));
        assertEquals((byte) -2, Remainder.remainder(Byte.MIN_VALUE, (byte) 7));
    }

    @Test
    public void remainderShort() {
        assertEquals((short) 6, Remainder.remainder((short) 19, (short) 13));
        assertEquals((short) -6, Remainder.remainder((short) -19, (short) 13));
        assertEquals((short) 9, Remainder.remainder(Short.MAX_VALUE, (short) 11));
    }

    @Test
    public void remainderFloat() {
        assertEquals(1.47f, Remainder.remainder(3.22f, 1.75f), 0.001f);
        assertEquals(1.47f, Remainder.remainder(3.22f, -1.75f), 0.001f);
        assertEquals(-1.47f, Remainder.remainder(-3.22f, 1.75f), 0.001f);
        assertEquals(-1.47f, Remainder.remainder(-3.22f, -1.75f), 0.001f);
        assertEquals(0.0f, Remainder.remainder(3.5f, 1.75f), 0.0f);
        assertEquals(0.0f, Remainder.remainder(0.0f, 1.75f), 0.0f);
    }

    @Test
    public void remainderFloatByZero() {
        assertThrows(ArithmeticException.class, () -> Remainder.remainder(3.22f, 0.0f));
    }

    @Test
    public void remainderFloatNanAndInfinity() {
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
    public void remainderDouble() {
        assertEquals(1.47, Remainder.remainder(3.22, 1.75), 0.001);
        assertEquals(1.47, Remainder.remainder(3.22, -1.75), 0.001);
        assertEquals(-1.47, Remainder.remainder(-3.22, 1.75), 0.001);
        assertEquals(-1.47, Remainder.remainder(-3.22, -1.75), 0.001);
        assertEquals(0.0, Remainder.remainder(0.0, 1.75), 0.0);
    }

    @Test
    public void remainderDoubleByZero() {
        assertThrows(ArithmeticException.class, () -> Remainder.remainder(3.22, 0.0));
    }

    @Test
    public void remainderDoubleNanAndInfinity() {
        assertTrue(Double.isNaN(Remainder.remainder(Double.NaN, 3.0)));
        assertTrue(Double.isNaN(Remainder.remainder(3.0, Double.NaN)));
        assertEquals(3.0, Remainder.remainder(3.0, Double.POSITIVE_INFINITY), 0.0);
        assertEquals(-3.0, Remainder.remainder(-3.0, Double.POSITIVE_INFINITY), 0.0);
        assertTrue(Double.isNaN(Remainder.remainder(Double.POSITIVE_INFINITY, 3.0)));
        assertTrue(Double.isNaN(Remainder.remainder(Double.NEGATIVE_INFINITY, 3.0)));
    }

    @Test
    public void remainderBigInteger() {
        assertEquals(BigInteger.valueOf(6), Remainder.remainder(BigInteger.valueOf(19), BigInteger.valueOf(13)));
        assertEquals(BigInteger.valueOf(6), Remainder.remainder(BigInteger.valueOf(19), BigInteger.valueOf(-13)));
        assertEquals(BigInteger.valueOf(-6), Remainder.remainder(BigInteger.valueOf(-19), BigInteger.valueOf(13)));
        assertEquals(BigInteger.valueOf(-6), Remainder.remainder(BigInteger.valueOf(-19), BigInteger.valueOf(-13)));
        assertEquals(BigInteger.ZERO, Remainder.remainder(BigInteger.valueOf(19), BigInteger.valueOf(19)));
        assertEquals(BigInteger.ZERO, Remainder.remainder(BigInteger.ZERO, BigInteger.valueOf(19)));
    }

    @Test
    public void remainderBigIntegerLarge() {
        BigInteger dividend = new BigInteger("203000745502000030060144252100");
        assertEquals(BigInteger.valueOf(59), Remainder.remainder(dividend, BigInteger.valueOf(97)));
        assertEquals(BigInteger.valueOf(59), Remainder.remainder(dividend, BigInteger.valueOf(-97)));
        assertEquals(BigInteger.valueOf(-59), Remainder.remainder(dividend.negate(), BigInteger.valueOf(97)));
        assertEquals(BigInteger.valueOf(-59), Remainder.remainder(dividend.negate(), BigInteger.valueOf(-97)));
    }

    @Test
    public void remainderBigIntegerByZero() {
        assertThrows(ArithmeticException.class,
                () -> Remainder.remainder(BigInteger.valueOf(19), BigInteger.ZERO));
    }

    @Test
    public void remainderBigDecimal() {
        assertEquals(BigDecimal.valueOf(1.47), Remainder.remainder(BigDecimal.valueOf(3.22), BigDecimal.valueOf(1.75)));
        assertEquals(BigDecimal.valueOf(1.47), Remainder.remainder(BigDecimal.valueOf(3.22), BigDecimal.valueOf(-1.75)));
        assertEquals(BigDecimal.valueOf(-1.47), Remainder.remainder(BigDecimal.valueOf(-3.22), BigDecimal.valueOf(1.75)));
        assertEquals(BigDecimal.valueOf(-1.47), Remainder.remainder(BigDecimal.valueOf(-3.22), BigDecimal.valueOf(-1.75)));
    }

    @Test
    public void remainderBigDecimalLarge() {
        BigDecimal dividend = new BigDecimal("203000745502000030060144252100");
        assertEquals(BigDecimal.valueOf(59), Remainder.remainder(dividend, BigDecimal.valueOf(97)));
    }

    @Test
    public void remainderBigDecimalByZero() {
        assertThrows(ArithmeticException.class,
                () -> Remainder.remainder(BigDecimal.valueOf(19), BigDecimal.ZERO));
    }

    @Test
    public void remainderBoxedNulls() {
        assertNull(Remainder.remainder((Byte) null, Byte.valueOf((byte) 13)));
        assertNull(Remainder.remainder(Byte.valueOf((byte) 13), (Byte) null));

        assertNull(Remainder.remainder((Short) null, Short.valueOf((short) 13)));
        assertNull(Remainder.remainder(Short.valueOf((short) 13), (Short) null));

        assertNull(Remainder.remainder((Integer) null, Integer.valueOf(13)));
        assertNull(Remainder.remainder(Integer.valueOf(13), (Integer) null));

        assertNull(Remainder.remainder((Long) null, Long.valueOf(13L)));
        assertNull(Remainder.remainder(Long.valueOf(13L), (Long) null));

        assertNull(Remainder.remainder((Float) null, Float.valueOf(1.75f)));
        assertNull(Remainder.remainder(Float.valueOf(1.75f), (Float) null));

        assertNull(Remainder.remainder((Double) null, Double.valueOf(1.75)));
        assertNull(Remainder.remainder(Double.valueOf(1.75), (Double) null));

        assertNull(Remainder.remainder((BigInteger) null, BigInteger.valueOf(13)));
        assertNull(Remainder.remainder(BigInteger.valueOf(13), (BigInteger) null));

        assertNull(Remainder.remainder((BigDecimal) null, BigDecimal.valueOf(13)));
        assertNull(Remainder.remainder(BigDecimal.valueOf(13), (BigDecimal) null));
    }

    @Test
    public void remainderBoxedHappyPath() {
        assertEquals(Byte.valueOf((byte) 6), Remainder.remainder(Byte.valueOf((byte) 19), Byte.valueOf((byte) 13)));
        assertEquals(Short.valueOf((short) 6), Remainder.remainder(Short.valueOf((short) 19), Short.valueOf((short) 13)));
        assertEquals(Integer.valueOf(1), Remainder.remainder(Integer.valueOf(7), Integer.valueOf(3)));
        assertEquals(Long.valueOf(1L), Remainder.remainder(Long.valueOf(7L), Long.valueOf(3L)));
        assertEquals(1.47f, Remainder.remainder(Float.valueOf(3.22f), Float.valueOf(1.75f)), 0.001f);
        assertEquals(1.47, Remainder.remainder(Double.valueOf(3.22), Double.valueOf(1.75)), 0.001);
    }
}
