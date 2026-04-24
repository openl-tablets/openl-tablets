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
        assertEquals(1L, Quotient.quotient(4.5f, 3.1f));
        assertEquals(1L, Quotient.quotient(4.5, 3.1));
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
        // Verify long precision is preserved when the quotient exceeds Integer.MAX_VALUE.
        assertEquals(3_000_000_000L, Quotient.quotient(9_000_000_000L, 3L));
        assertEquals(Long.MAX_VALUE, Quotient.quotient(Long.MAX_VALUE, 1L));
        assertEquals(Long.MAX_VALUE / 7L, Quotient.quotient(Long.MAX_VALUE, 7L));
    }

    @Test
    public void quotientFloatLarge() {
        // Quotient exceeds int's 2^31 range. Float has ~7 significant digits, so the exact
        // value varies by float rounding; we just verify it lands in a ~1% tolerance band and
        // fits in long precision (not truncated to int).
        long q = Quotient.quotient(1.0e10f, 3.0f);
        assertTrue(q > Integer.MAX_VALUE, "expected long-magnitude result, got " + q);
        assertEquals(3_333_333_333L, q, 1_000_000L);
    }

    @Test
    public void quotientDoubleLarge() {
        // Double has ~15-16 significant digits, so 1e15 / 3 is exact.
        assertEquals(333_333_333_333_333L, Quotient.quotient(1.0e15, 3.0));
        assertEquals(-333_333_333_333_333L, Quotient.quotient(-1.0e15, 3.0));
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
        assertEquals(1L, Quotient.quotient(4.5f, 3.1f));
        assertEquals(-1L, Quotient.quotient(4.5f, -3.1f));
        assertEquals(-1L, Quotient.quotient(-4.5f, 3.1f));
        assertEquals(1L, Quotient.quotient(-4.5f, -3.1f));
        assertEquals(0L, Quotient.quotient(0.0f, 3.1f));
    }

    @Test
    public void quotientFloatByZero() {
        assertThrows(ArithmeticException.class, () -> Quotient.quotient(4.5f, 0.0f));
    }

    @Test
    public void quotientFloatNanAndInfinityThrow() {
        // No integer representation of NaN or Infinity.
        assertThrows(ArithmeticException.class, () -> Quotient.quotient(Float.NaN, 3.0f));
        assertThrows(ArithmeticException.class, () -> Quotient.quotient(3.0f, Float.NaN));
        assertThrows(ArithmeticException.class, () -> Quotient.quotient(Float.POSITIVE_INFINITY, 3.0f));
        assertThrows(ArithmeticException.class, () -> Quotient.quotient(Float.NEGATIVE_INFINITY, 3.0f));
        assertThrows(ArithmeticException.class, () -> Quotient.quotient(3.0f, Float.POSITIVE_INFINITY));
        assertThrows(ArithmeticException.class, () -> Quotient.quotient(3.0f, Float.NEGATIVE_INFINITY));
    }

    @Test
    public void quotientDouble() {
        assertEquals(1L, Quotient.quotient(4.5, 3.1));
        assertEquals(-1L, Quotient.quotient(4.5, -3.1));
        assertEquals(-1L, Quotient.quotient(-4.5, 3.1));
        assertEquals(1L, Quotient.quotient(-4.5, -3.1));
        assertEquals(0L, Quotient.quotient(0.0, 3.1));
    }

    @Test
    public void quotientDoubleByZero() {
        assertThrows(ArithmeticException.class, () -> Quotient.quotient(4.5, 0.0));
    }

    @Test
    public void quotientDoubleNanAndInfinityThrow() {
        assertThrows(ArithmeticException.class, () -> Quotient.quotient(Double.NaN, 3.0));
        assertThrows(ArithmeticException.class, () -> Quotient.quotient(3.0, Double.NaN));
        assertThrows(ArithmeticException.class, () -> Quotient.quotient(Double.POSITIVE_INFINITY, 3.0));
        assertThrows(ArithmeticException.class, () -> Quotient.quotient(Double.NEGATIVE_INFINITY, 3.0));
        assertThrows(ArithmeticException.class, () -> Quotient.quotient(3.0, Double.POSITIVE_INFINITY));
        assertThrows(ArithmeticException.class, () -> Quotient.quotient(3.0, Double.NEGATIVE_INFINITY));
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
        // Quotient far exceeds Long.MAX_VALUE — must preserve full precision.
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
        assertEquals(BigInteger.valueOf(1), Quotient.quotient(BigDecimal.valueOf(4.5), BigDecimal.valueOf(3.1)));
        assertEquals(BigInteger.valueOf(-3), Quotient.quotient(BigDecimal.valueOf(-10), BigDecimal.valueOf(3)));
        assertEquals(BigInteger.valueOf(-3), Quotient.quotient(BigDecimal.valueOf(10), BigDecimal.valueOf(-3)));
        assertEquals(BigInteger.valueOf(3), Quotient.quotient(BigDecimal.valueOf(-10), BigDecimal.valueOf(-3)));
    }

    @Test
    public void quotientBigDecimalLarge() {
        BigDecimal dividend = new BigDecimal("203000745502000030060144252100");
        BigInteger expected = new BigInteger("2092791190742268351135507753");
        assertEquals(expected, Quotient.quotient(dividend, BigDecimal.valueOf(97)));
    }

    @Test
    public void quotientBigDecimalByZero() {
        assertThrows(ArithmeticException.class,
                () -> Quotient.quotient(BigDecimal.valueOf(19), BigDecimal.ZERO));
    }

    @Test
    public void quotientBoxedNulls() {
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
    public void quotientBoxedHappyPath() {
        assertEquals(Integer.valueOf(3), Quotient.quotient((byte) 19, (byte) 5));
        assertEquals(Integer.valueOf(3), Quotient.quotient((short) 19, (short) 5));
        assertEquals(Integer.valueOf(2), Quotient.quotient(5, 2));
        assertEquals(Long.valueOf(2L), Quotient.quotient(5L, 2L));
        assertEquals(Long.valueOf(1L), Quotient.quotient(4.5f, 3.1f));
        assertEquals(Long.valueOf(1L), Quotient.quotient(4.5, 3.1));
    }
}
