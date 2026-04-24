package org.openl.rules.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;

class QuotientTest {

    @Test
    void excelExamples() {
        assertEquals(2, Quotient.quotient(5, 2));
        assertEquals(1.0f, Quotient.quotient(4.5f, 3.1f), 0.0f);
        assertEquals(1.0, Quotient.quotient(4.5, 3.1), 0.0);
        assertEquals(-3, Quotient.quotient(-10, 3));
    }

    @Test
    void quotientInt() {
        assertEquals(3, Quotient.quotient(19, 5));
        assertEquals(-3, Quotient.quotient(19, -5));
        assertEquals(-3, Quotient.quotient(-19, 5));
        assertEquals(3, Quotient.quotient(-19, -5));
        assertEquals(0, Quotient.quotient(0, 19));
        assertEquals(0, Quotient.quotient(3, 5));
        assertEquals(0, Quotient.quotient(-3, 5));
    }

    @Test
    void quotientIntByZero() {
        assertThrows(ArithmeticException.class, () -> Quotient.quotient(5, 0));
    }

    @Test
    void quotientLong() {
        assertEquals(3L, Quotient.quotient(19L, 5L));
        assertEquals(-3L, Quotient.quotient(-19L, 5L));
        assertEquals(0L, Quotient.quotient(5L, 19L));
        // Long.MIN_VALUE / -1 would overflow mathematically; Java returns Long.MIN_VALUE.
        assertEquals(Long.MIN_VALUE, Quotient.quotient(Long.MIN_VALUE, -1L));
    }

    @Test
    void quotientLongLarge() {
        assertEquals(3_000_000_000L, Quotient.quotient(9_000_000_000L, 3L));
        assertEquals(Long.MAX_VALUE, Quotient.quotient(Long.MAX_VALUE, 1L));
        assertEquals(Long.MAX_VALUE / 7L, Quotient.quotient(Long.MAX_VALUE, 7L));
    }

    @Test
    void quotientDoubleLarge() {
        // Double has ~15-16 significant digits — integer result with zero fractional part.
        assertEquals(3.33333333333333E14, Quotient.quotient(1.0e15, 3.0), 0.0);
        assertEquals(-3.33333333333333E14, Quotient.quotient(-1.0e15, 3.0), 0.0);
    }

    @Test
    void quotientLongByZero() {
        assertThrows(ArithmeticException.class, () -> Quotient.quotient(5L, 0L));
    }

    @Test
    void quotientByte() {
        assertEquals(3, Quotient.quotient((byte) 19, (byte) 5));
        assertEquals(-3, Quotient.quotient((byte) 19, (byte) -5));
        assertEquals(18, Quotient.quotient(Byte.MAX_VALUE, (byte) 7));
    }

    @Test
    void quotientShort() {
        assertEquals(3, Quotient.quotient((short) 19, (short) 5));
        assertEquals(-3, Quotient.quotient((short) 19, (short) -5));
        assertEquals(2978, Quotient.quotient(Short.MAX_VALUE, (short) 11));
    }

    @Test
    void quotientFloat() {
        assertEquals(1.0f, Quotient.quotient(4.5f, 3.1f), 0.0f);
        assertEquals(-1.0f, Quotient.quotient(4.5f, -3.1f), 0.0f);
        assertEquals(-1.0f, Quotient.quotient(-4.5f, 3.1f), 0.0f);
        assertEquals(1.0f, Quotient.quotient(-4.5f, -3.1f), 0.0f);
        assertEquals(0.0f, Quotient.quotient(0.0f, 3.1f), 0.0f);
        // Result is integral — zero fractional part.
        float q = Quotient.quotient(1.0e10f, 7.0f);
        assertTrue(Float.isFinite(q));
        assertEquals(q, (float) Math.floor(q), 0.0f);
    }

    @Test
    void quotientFloatByZero() {
        // Float / zero follows IEEE 754.
        assertEquals(Float.POSITIVE_INFINITY, Quotient.quotient(4.5f, 0.0f), 0.0f);
        assertEquals(Float.NEGATIVE_INFINITY, Quotient.quotient(-4.5f, 0.0f), 0.0f);
        assertTrue(Float.isNaN(Quotient.quotient(0.0f, 0.0f)));
    }

    @Test
    void quotientFloatNanAndInfinity() {
        assertTrue(Float.isNaN(Quotient.quotient(Float.NaN, 3.0f)));
        assertTrue(Float.isNaN(Quotient.quotient(3.0f, Float.NaN)));
        assertEquals(Float.POSITIVE_INFINITY, Quotient.quotient(Float.POSITIVE_INFINITY, 3.0f), 0.0f);
        assertEquals(Float.NEGATIVE_INFINITY, Quotient.quotient(Float.NEGATIVE_INFINITY, 3.0f), 0.0f);
        assertEquals(0.0f, Quotient.quotient(3.0f, Float.POSITIVE_INFINITY), 0.0f);
        assertEquals(0.0f, Quotient.quotient(3.0f, Float.NEGATIVE_INFINITY), 0.0f);
    }

    @Test
    void quotientDouble() {
        assertEquals(1.0, Quotient.quotient(4.5, 3.1), 0.0);
        assertEquals(-1.0, Quotient.quotient(4.5, -3.1), 0.0);
        assertEquals(-1.0, Quotient.quotient(-4.5, 3.1), 0.0);
        assertEquals(1.0, Quotient.quotient(-4.5, -3.1), 0.0);
        assertEquals(0.0, Quotient.quotient(0.0, 3.1), 0.0);
    }

    @Test
    void quotientDoubleByZero() {
        assertEquals(Double.POSITIVE_INFINITY, Quotient.quotient(4.5, 0.0), 0.0);
        assertEquals(Double.NEGATIVE_INFINITY, Quotient.quotient(-4.5, 0.0), 0.0);
        assertTrue(Double.isNaN(Quotient.quotient(0.0, 0.0)));
    }

    @Test
    void quotientDoubleNanAndInfinity() {
        assertTrue(Double.isNaN(Quotient.quotient(Double.NaN, 3.0)));
        assertTrue(Double.isNaN(Quotient.quotient(3.0, Double.NaN)));
        assertEquals(Double.POSITIVE_INFINITY, Quotient.quotient(Double.POSITIVE_INFINITY, 3.0), 0.0);
        assertEquals(Double.NEGATIVE_INFINITY, Quotient.quotient(Double.NEGATIVE_INFINITY, 3.0), 0.0);
        assertEquals(0.0, Quotient.quotient(3.0, Double.POSITIVE_INFINITY), 0.0);
        assertEquals(0.0, Quotient.quotient(3.0, Double.NEGATIVE_INFINITY), 0.0);
    }

    @Test
    void quotientBigInteger() {
        assertEquals(BigInteger.valueOf(3), Quotient.quotient(BigInteger.valueOf(19), BigInteger.valueOf(5)));
        assertEquals(BigInteger.valueOf(-3), Quotient.quotient(BigInteger.valueOf(-10), BigInteger.valueOf(3)));
        assertEquals(BigInteger.valueOf(-3), Quotient.quotient(BigInteger.valueOf(10), BigInteger.valueOf(-3)));
        assertEquals(BigInteger.valueOf(3), Quotient.quotient(BigInteger.valueOf(-10), BigInteger.valueOf(-3)));
        assertEquals(new BigInteger("100000000000000000000"),
                Quotient.quotient(new BigInteger("100000000000000000000"), BigInteger.ONE));
    }

    @Test
    void quotientBigIntegerLarge() {
        BigInteger dividend = new BigInteger("203000745502000030060144252100");
        BigInteger expected = new BigInteger("2092791190742268351135507753");
        assertEquals(expected, Quotient.quotient(dividend, BigInteger.valueOf(97)));
        assertEquals(expected.negate(), Quotient.quotient(dividend, BigInteger.valueOf(-97)));
        assertEquals(expected.negate(), Quotient.quotient(dividend.negate(), BigInteger.valueOf(97)));
        assertEquals(expected, Quotient.quotient(dividend.negate(), BigInteger.valueOf(-97)));
    }

    @Test
    void quotientBigIntegerByZero() {
        assertThrows(ArithmeticException.class,
                () -> Quotient.quotient(BigInteger.valueOf(19), BigInteger.ZERO));
    }

    @Test
    void quotientBigDecimal() {
        assertEquals(0, BigDecimal.valueOf(1).compareTo(
                Quotient.quotient(BigDecimal.valueOf(4.5), BigDecimal.valueOf(3.1))));
        assertEquals(0, BigDecimal.valueOf(-3).compareTo(
                Quotient.quotient(BigDecimal.valueOf(-10), BigDecimal.valueOf(3))));
        assertEquals(0, BigDecimal.valueOf(-3).compareTo(
                Quotient.quotient(BigDecimal.valueOf(10), BigDecimal.valueOf(-3))));
        assertEquals(0, BigDecimal.valueOf(3).compareTo(
                Quotient.quotient(BigDecimal.valueOf(-10), BigDecimal.valueOf(-3))));
    }

    @Test
    void quotientBigDecimalLarge() {
        BigDecimal dividend = new BigDecimal("203000745502000030060144252100");
        BigDecimal expected = new BigDecimal("2092791190742268351135507753");
        assertEquals(0, expected.compareTo(Quotient.quotient(dividend, BigDecimal.valueOf(97))));
    }

    @Test
    void quotientBigDecimalByZero() {
        assertThrows(ArithmeticException.class,
                () -> Quotient.quotient(BigDecimal.valueOf(19), BigDecimal.ZERO));
    }

    @Test
    void quotientBoxedNulls() {
        assertNull(Quotient.quotient(null, (Byte) null));
        assertNull(Quotient.quotient(null, (byte) 3));
        assertNull(Quotient.quotient((byte) 19, null));

        assertNull(Quotient.quotient(null, (Short) null));
        assertNull(Quotient.quotient(null, (short) 3));
        assertNull(Quotient.quotient((short) 19, null));

        assertNull(Quotient.quotient(null, (Integer) null));
        assertNull(Quotient.quotient(null, 3));
        assertNull(Quotient.quotient(19, null));

        assertNull(Quotient.quotient(null, (Long) null));
        assertNull(Quotient.quotient(null, 3L));
        assertNull(Quotient.quotient(19L, null));

        assertNull(Quotient.quotient(null, (Float) null));
        assertNull(Quotient.quotient(null, 3.1f));
        assertNull(Quotient.quotient(4.5f, null));

        assertNull(Quotient.quotient(null, (Double) null));
        assertNull(Quotient.quotient(null, 3.1));
        assertNull(Quotient.quotient(4.5, null));

        assertNull(Quotient.quotient(null, BigInteger.valueOf(3)));
        assertNull(Quotient.quotient(BigInteger.valueOf(19), null));
        assertNull(Quotient.quotient(null, (BigInteger) null));

        assertNull(Quotient.quotient(null, BigDecimal.valueOf(3)));
        assertNull(Quotient.quotient(BigDecimal.valueOf(19), null));
        assertNull(Quotient.quotient(null, (BigDecimal) null));
    }

    @Test
    void quotientBoxedHappyPath() {
        assertEquals(3, Quotient.quotient((byte) 19, (byte) 5));
        assertEquals(3, Quotient.quotient((short) 19, (short) 5));
        assertEquals(2, Quotient.quotient(5, 2));
        assertEquals(2L, Quotient.quotient(5L, 2L));
        assertEquals(1.0f, Quotient.quotient(4.5f, 3.1f), 0.0f);
        assertEquals(1.0, Quotient.quotient(4.5, 3.1), 0.0);
    }
}
