package org.openl.rules.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;

public class QuotientTest {

    @Test
    public void excelExamples() {
        assertEquals(2, Quotient.quotient(5, 2));
        assertEquals(1.0f, Quotient.quotient(4.5f, 3.1f), 0.0f);
        assertEquals(1.0, Quotient.quotient(4.5, 3.1), 0.0);
        assertEquals(-3, Quotient.quotient(-10, 3));
    }

    @Test
    public void quotientInt() {
        assertEquals(3, Quotient.quotient(19, 5));
        assertEquals(-3, Quotient.quotient(19, -5));
        assertEquals(-3, Quotient.quotient(-19, 5));
        assertEquals(3, Quotient.quotient(-19, -5));
        assertEquals(0, Quotient.quotient(0, 19));
        assertEquals(0, Quotient.quotient(3, 5));
        assertEquals(0, Quotient.quotient(-3, 5));
    }

    @Test
    public void quotientIntByZero() {
        assertThrows(ArithmeticException.class, () -> Quotient.quotient(5, 0));
    }

    @Test
    public void quotientLong() {
        assertEquals(3L, Quotient.quotient(19L, 5L));
        assertEquals(-3L, Quotient.quotient(-19L, 5L));
        assertEquals(0L, Quotient.quotient(5L, 19L));
        // Long.MIN_VALUE / -1 would overflow mathematically; Java returns Long.MIN_VALUE.
        assertEquals(Long.MIN_VALUE, Quotient.quotient(Long.MIN_VALUE, -1L));
    }

    @Test
    public void quotientLongLarge() {
        assertEquals(3_000_000_000L, Quotient.quotient(9_000_000_000L, 3L));
        assertEquals(Long.MAX_VALUE, Quotient.quotient(Long.MAX_VALUE, 1L));
        assertEquals(Long.MAX_VALUE / 7L, Quotient.quotient(Long.MAX_VALUE, 7L));
    }

    @Test
    public void quotientDoubleLarge() {
        // Double has ~15-16 significant digits — integer result with zero fractional part.
        assertEquals(3.33333333333333E14, Quotient.quotient(1.0e15, 3.0), 0.0);
        assertEquals(-3.33333333333333E14, Quotient.quotient(-1.0e15, 3.0), 0.0);
    }

    @Test
    public void quotientLongByZero() {
        assertThrows(ArithmeticException.class, () -> Quotient.quotient(5L, 0L));
    }

    @Test
    public void quotientByte() {
        assertEquals(3, Quotient.quotient((byte) 19, (byte) 5));
        assertEquals(-3, Quotient.quotient((byte) 19, (byte) -5));
        assertEquals(18, Quotient.quotient(Byte.MAX_VALUE, (byte) 7));
    }

    @Test
    public void quotientShort() {
        assertEquals(3, Quotient.quotient((short) 19, (short) 5));
        assertEquals(-3, Quotient.quotient((short) 19, (short) -5));
        assertEquals(2978, Quotient.quotient(Short.MAX_VALUE, (short) 11));
    }

    @Test
    public void quotientFloat() {
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
    public void quotientFloatByZero() {
        // Float / zero follows IEEE 754.
        assertEquals(Float.POSITIVE_INFINITY, Quotient.quotient(4.5f, 0.0f), 0.0f);
        assertEquals(Float.NEGATIVE_INFINITY, Quotient.quotient(-4.5f, 0.0f), 0.0f);
        assertTrue(Float.isNaN(Quotient.quotient(0.0f, 0.0f)));
    }

    @Test
    public void quotientFloatNanAndInfinity() {
        assertTrue(Float.isNaN(Quotient.quotient(Float.NaN, 3.0f)));
        assertTrue(Float.isNaN(Quotient.quotient(3.0f, Float.NaN)));
        assertEquals(Float.POSITIVE_INFINITY, Quotient.quotient(Float.POSITIVE_INFINITY, 3.0f), 0.0f);
        assertEquals(Float.NEGATIVE_INFINITY, Quotient.quotient(Float.NEGATIVE_INFINITY, 3.0f), 0.0f);
        assertEquals(0.0f, Quotient.quotient(3.0f, Float.POSITIVE_INFINITY), 0.0f);
        assertEquals(0.0f, Quotient.quotient(3.0f, Float.NEGATIVE_INFINITY), 0.0f);
    }

    @Test
    public void quotientDouble() {
        assertEquals(1.0, Quotient.quotient(4.5, 3.1), 0.0);
        assertEquals(-1.0, Quotient.quotient(4.5, -3.1), 0.0);
        assertEquals(-1.0, Quotient.quotient(-4.5, 3.1), 0.0);
        assertEquals(1.0, Quotient.quotient(-4.5, -3.1), 0.0);
        assertEquals(0.0, Quotient.quotient(0.0, 3.1), 0.0);
    }

    @Test
    public void quotientDoubleByZero() {
        assertEquals(Double.POSITIVE_INFINITY, Quotient.quotient(4.5, 0.0), 0.0);
        assertEquals(Double.NEGATIVE_INFINITY, Quotient.quotient(-4.5, 0.0), 0.0);
        assertTrue(Double.isNaN(Quotient.quotient(0.0, 0.0)));
    }

    @Test
    public void quotientDoubleNanAndInfinity() {
        assertTrue(Double.isNaN(Quotient.quotient(Double.NaN, 3.0)));
        assertTrue(Double.isNaN(Quotient.quotient(3.0, Double.NaN)));
        assertEquals(Double.POSITIVE_INFINITY, Quotient.quotient(Double.POSITIVE_INFINITY, 3.0), 0.0);
        assertEquals(Double.NEGATIVE_INFINITY, Quotient.quotient(Double.NEGATIVE_INFINITY, 3.0), 0.0);
        assertEquals(0.0, Quotient.quotient(3.0, Double.POSITIVE_INFINITY), 0.0);
        assertEquals(0.0, Quotient.quotient(3.0, Double.NEGATIVE_INFINITY), 0.0);
    }

    @Test
    public void quotientBigInteger() {
        assertEquals(BigInteger.valueOf(3), Quotient.quotient(BigInteger.valueOf(19), BigInteger.valueOf(5)));
        assertEquals(BigInteger.valueOf(-3), Quotient.quotient(BigInteger.valueOf(-10), BigInteger.valueOf(3)));
        assertEquals(BigInteger.valueOf(-3), Quotient.quotient(BigInteger.valueOf(10), BigInteger.valueOf(-3)));
        assertEquals(BigInteger.valueOf(3), Quotient.quotient(BigInteger.valueOf(-10), BigInteger.valueOf(-3)));
        assertEquals(new BigInteger("100000000000000000000"),
                Quotient.quotient(new BigInteger("100000000000000000000"), BigInteger.ONE));
    }

    @Test
    public void quotientBigIntegerLarge() {
        BigInteger dividend = new BigInteger("203000745502000030060144252100");
        BigInteger expected = new BigInteger("2092791190742268351135507753");
        assertEquals(expected, Quotient.quotient(dividend, BigInteger.valueOf(97)));
        assertEquals(expected.negate(), Quotient.quotient(dividend, BigInteger.valueOf(-97)));
        assertEquals(expected.negate(), Quotient.quotient(dividend.negate(), BigInteger.valueOf(97)));
        assertEquals(expected, Quotient.quotient(dividend.negate(), BigInteger.valueOf(-97)));
    }

    @Test
    public void quotientBigIntegerByZero() {
        assertThrows(ArithmeticException.class,
                () -> Quotient.quotient(BigInteger.valueOf(19), BigInteger.ZERO));
    }

    @Test
    public void quotientBigDecimal() {
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
    public void quotientBigDecimalLarge() {
        BigDecimal dividend = new BigDecimal("203000745502000030060144252100");
        BigDecimal expected = new BigDecimal("2092791190742268351135507753");
        assertEquals(0, expected.compareTo(Quotient.quotient(dividend, BigDecimal.valueOf(97))));
    }

    @Test
    public void quotientBigDecimalByZero() {
        assertThrows(ArithmeticException.class,
                () -> Quotient.quotient(BigDecimal.valueOf(19), BigDecimal.ZERO));
    }

    @Test
    public void quotientBoxedNulls() {
        assertNull(Quotient.quotient((Byte) null, (Byte) null));
        assertNull(Quotient.quotient((Byte) null, Byte.valueOf((byte) 3)));
        assertNull(Quotient.quotient(Byte.valueOf((byte) 19), (Byte) null));

        assertNull(Quotient.quotient((Short) null, (Short) null));
        assertNull(Quotient.quotient((Short) null, Short.valueOf((short) 3)));
        assertNull(Quotient.quotient(Short.valueOf((short) 19), (Short) null));

        assertNull(Quotient.quotient((Integer) null, (Integer) null));
        assertNull(Quotient.quotient((Integer) null, Integer.valueOf(3)));
        assertNull(Quotient.quotient(Integer.valueOf(19), (Integer) null));

        assertNull(Quotient.quotient((Long) null, (Long) null));
        assertNull(Quotient.quotient((Long) null, Long.valueOf(3L)));
        assertNull(Quotient.quotient(Long.valueOf(19L), (Long) null));

        assertNull(Quotient.quotient((Float) null, (Float) null));
        assertNull(Quotient.quotient((Float) null, Float.valueOf(3.1f)));
        assertNull(Quotient.quotient(Float.valueOf(4.5f), (Float) null));

        assertNull(Quotient.quotient((Double) null, (Double) null));
        assertNull(Quotient.quotient((Double) null, Double.valueOf(3.1)));
        assertNull(Quotient.quotient(Double.valueOf(4.5), (Double) null));

        assertNull(Quotient.quotient((BigInteger) null, BigInteger.valueOf(3)));
        assertNull(Quotient.quotient(BigInteger.valueOf(19), (BigInteger) null));
        assertNull(Quotient.quotient((BigInteger) null, (BigInteger) null));

        assertNull(Quotient.quotient((BigDecimal) null, BigDecimal.valueOf(3)));
        assertNull(Quotient.quotient(BigDecimal.valueOf(19), (BigDecimal) null));
        assertNull(Quotient.quotient((BigDecimal) null, (BigDecimal) null));
    }

    @Test
    public void quotientBoxedHappyPath() {
        assertEquals(Integer.valueOf(3), Quotient.quotient(Byte.valueOf((byte) 19), Byte.valueOf((byte) 5)));
        assertEquals(Integer.valueOf(3), Quotient.quotient(Short.valueOf((short) 19), Short.valueOf((short) 5)));
        assertEquals(Integer.valueOf(2), Quotient.quotient(Integer.valueOf(5), Integer.valueOf(2)));
        assertEquals(Long.valueOf(2L), Quotient.quotient(Long.valueOf(5L), Long.valueOf(2L)));
        assertEquals(1.0f, Quotient.quotient(Float.valueOf(4.5f), Float.valueOf(3.1f)), 0.0f);
        assertEquals(1.0, Quotient.quotient(Double.valueOf(4.5), Double.valueOf(3.1)), 0.0);
    }
}
