package org.openl.rules.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;

class FloorDivTest {

    @Test
    void canonicalExamples() {
        // floor(-10/3) = -4; trunc = -3. This is the defining difference from Quotient.
        assertEquals(-4, FloorDiv.floorDiv(-10, 3));
        assertEquals(2, FloorDiv.floorDiv(7, 3));
        assertEquals(-4, FloorDiv.floorDiv(7, -2));
        assertEquals(3, FloorDiv.floorDiv(-7, -2));
    }

    @Test
    void floorDivInt() {
        // Same sign → floor == truncation.
        assertEquals(3, FloorDiv.floorDiv(19, 5));
        assertEquals(3, FloorDiv.floorDiv(-19, -5));
        // Different signs, non-zero remainder → floor == trunc - 1.
        assertEquals(-4, FloorDiv.floorDiv(19, -5));
        assertEquals(-4, FloorDiv.floorDiv(-19, 5));
        // Exact divisibility regardless of signs.
        assertEquals(-3, FloorDiv.floorDiv(-15, 5));
        assertEquals(3, FloorDiv.floorDiv(-15, -5));
        assertEquals(0, FloorDiv.floorDiv(0, 5));
    }

    @Test
    void floorDivIntByZero() {
        assertThrows(ArithmeticException.class, () -> FloorDiv.floorDiv(5, 0));
    }

    @Test
    void floorDivLong() {
        assertEquals(3L, FloorDiv.floorDiv(19L, 5L));
        assertEquals(-4L, FloorDiv.floorDiv(-19L, 5L));
        assertEquals(-4L, FloorDiv.floorDiv(19L, -5L));
        assertEquals(3L, FloorDiv.floorDiv(-19L, -5L));
        // Large-magnitude: long precision preserved.
        assertEquals(3_000_000_000L, FloorDiv.floorDiv(9_000_000_000L, 3L));
    }

    @Test
    void floorDivLongByZero() {
        assertThrows(ArithmeticException.class, () -> FloorDiv.floorDiv(5L, 0L));
    }

    @Test
    void floorDivByte() {
        assertEquals(3, FloorDiv.floorDiv((byte) 19, (byte) 5));
        assertEquals(-4, FloorDiv.floorDiv((byte) 19, (byte) -5));
        assertEquals(-4, FloorDiv.floorDiv((byte) -19, (byte) 5));
    }

    @Test
    void floorDivShort() {
        assertEquals(3, FloorDiv.floorDiv((short) 19, (short) 5));
        assertEquals(-4, FloorDiv.floorDiv((short) 19, (short) -5));
        assertEquals(-4, FloorDiv.floorDiv((short) -19, (short) 5));
    }

    @Test
    void floorDivFloat() {
        assertEquals(1.0f, FloorDiv.floorDiv(4.5f, 3.1f), 0.0f);
        assertEquals(-2.0f, FloorDiv.floorDiv(4.5f, -3.1f), 0.0f);
        assertEquals(-2.0f, FloorDiv.floorDiv(-4.5f, 3.1f), 0.0f);
        assertEquals(1.0f, FloorDiv.floorDiv(-4.5f, -3.1f), 0.0f);
        assertEquals(0.0f, FloorDiv.floorDiv(0.0f, 3.1f), 0.0f);
    }

    @Test
    void floorDivFloatByZero() {
        // IEEE 754: x/0 → ±Infinity, floor(±Inf) = ±Inf; 0/0 → NaN.
        assertEquals(Float.POSITIVE_INFINITY, FloorDiv.floorDiv(4.5f, 0.0f), 0.0f);
        assertEquals(Float.NEGATIVE_INFINITY, FloorDiv.floorDiv(-4.5f, 0.0f), 0.0f);
        assertTrue(Float.isNaN(FloorDiv.floorDiv(0.0f, 0.0f)));
    }

    @Test
    void floorDivFloatNanAndInfinity() {
        assertTrue(Float.isNaN(FloorDiv.floorDiv(Float.NaN, 3.0f)));
        assertTrue(Float.isNaN(FloorDiv.floorDiv(3.0f, Float.NaN)));
        assertEquals(Float.POSITIVE_INFINITY, FloorDiv.floorDiv(Float.POSITIVE_INFINITY, 3.0f), 0.0f);
        assertEquals(Float.NEGATIVE_INFINITY, FloorDiv.floorDiv(Float.NEGATIVE_INFINITY, 3.0f), 0.0f);
        assertEquals(0.0f, FloorDiv.floorDiv(3.0f, Float.POSITIVE_INFINITY), 0.0f);
        // 3 / -Inf = -0.0f; Math.floor preserves the signed zero.
        assertEquals(0.0f, FloorDiv.floorDiv(3.0f, Float.NEGATIVE_INFINITY), 0.0f);
    }

    @Test
    void floorDivDouble() {
        assertEquals(1.0, FloorDiv.floorDiv(4.5, 3.1), 0.0);
        assertEquals(-2.0, FloorDiv.floorDiv(4.5, -3.1), 0.0);
        assertEquals(-2.0, FloorDiv.floorDiv(-4.5, 3.1), 0.0);
        assertEquals(1.0, FloorDiv.floorDiv(-4.5, -3.1), 0.0);
    }

    @Test
    void floorDivDoubleByZero() {
        assertEquals(Double.POSITIVE_INFINITY, FloorDiv.floorDiv(4.5, 0.0), 0.0);
        assertEquals(Double.NEGATIVE_INFINITY, FloorDiv.floorDiv(-4.5, 0.0), 0.0);
        assertTrue(Double.isNaN(FloorDiv.floorDiv(0.0, 0.0)));
    }

    @Test
    void floorDivDoubleNanAndInfinity() {
        assertTrue(Double.isNaN(FloorDiv.floorDiv(Double.NaN, 3.0)));
        assertTrue(Double.isNaN(FloorDiv.floorDiv(3.0, Double.NaN)));
        assertEquals(Double.POSITIVE_INFINITY, FloorDiv.floorDiv(Double.POSITIVE_INFINITY, 3.0), 0.0);
        assertEquals(Double.NEGATIVE_INFINITY, FloorDiv.floorDiv(Double.NEGATIVE_INFINITY, 3.0), 0.0);
    }

    @Test
    void floorDivBigInteger() {
        // Same sign → truncation == floor.
        assertEquals(BigInteger.valueOf(3), FloorDiv.floorDiv(BigInteger.valueOf(19), BigInteger.valueOf(5)));
        assertEquals(BigInteger.valueOf(3), FloorDiv.floorDiv(BigInteger.valueOf(-19), BigInteger.valueOf(-5)));
        // Different signs + remainder → fallback subtracts one.
        assertEquals(BigInteger.valueOf(-4), FloorDiv.floorDiv(BigInteger.valueOf(19), BigInteger.valueOf(-5)));
        assertEquals(BigInteger.valueOf(-4), FloorDiv.floorDiv(BigInteger.valueOf(-19), BigInteger.valueOf(5)));
        // Exact divisibility across signs → no adjustment.
        assertEquals(BigInteger.valueOf(-3), FloorDiv.floorDiv(BigInteger.valueOf(-15), BigInteger.valueOf(5)));
        assertEquals(BigInteger.valueOf(-3), FloorDiv.floorDiv(BigInteger.valueOf(15), BigInteger.valueOf(-5)));
        // Zero dividend.
        assertEquals(BigInteger.ZERO, FloorDiv.floorDiv(BigInteger.ZERO, BigInteger.valueOf(5)));
    }

    @Test
    void floorDivBigIntegerLarge() {
        // floor(-N/97) where N is > long range. For N positive, divisor positive: floor == trunc.
        BigInteger dividend = new BigInteger("203000745502000030060144252100");
        BigInteger expected = new BigInteger("2092791190742268351135507753");
        assertEquals(expected, FloorDiv.floorDiv(dividend, BigInteger.valueOf(97)));
        // Opposite-sign case: remainder is 59 (non-zero), so floor == trunc - 1.
        assertEquals(expected.negate().subtract(BigInteger.ONE),
                FloorDiv.floorDiv(dividend, BigInteger.valueOf(-97)));
    }

    @Test
    void floorDivBigIntegerByZero() {
        assertThrows(ArithmeticException.class,
                () -> FloorDiv.floorDiv(BigInteger.valueOf(19), BigInteger.ZERO));
    }

    @Test
    void floorDivBigDecimal() {
        assertEquals(0, BigDecimal.valueOf(1).compareTo(
                FloorDiv.floorDiv(BigDecimal.valueOf(4.5), BigDecimal.valueOf(3.1))));
        assertEquals(0, BigDecimal.valueOf(-2).compareTo(
                FloorDiv.floorDiv(BigDecimal.valueOf(4.5), BigDecimal.valueOf(-3.1))));
        assertEquals(0, BigDecimal.valueOf(-2).compareTo(
                FloorDiv.floorDiv(BigDecimal.valueOf(-4.5), BigDecimal.valueOf(3.1))));
        assertEquals(0, BigDecimal.valueOf(1).compareTo(
                FloorDiv.floorDiv(BigDecimal.valueOf(-4.5), BigDecimal.valueOf(-3.1))));
        // floor(-10/3) = -4
        assertEquals(0, BigDecimal.valueOf(-4).compareTo(
                FloorDiv.floorDiv(BigDecimal.valueOf(-10), BigDecimal.valueOf(3))));
    }

    @Test
    void floorDivBigDecimalByZero() {
        assertThrows(ArithmeticException.class,
                () -> FloorDiv.floorDiv(BigDecimal.valueOf(19), BigDecimal.ZERO));
    }

    @Test
    void floorDivBoxedNulls() {
        assertNull(FloorDiv.floorDiv(null, (Byte) null));
        assertNull(FloorDiv.floorDiv(null, (byte) 3));
        assertNull(FloorDiv.floorDiv((byte) 19, null));
        assertNull(FloorDiv.floorDiv(null, (short) 3));
        assertNull(FloorDiv.floorDiv((short) 19, null));
        assertNull(FloorDiv.floorDiv(null, 3));
        assertNull(FloorDiv.floorDiv(19, null));
        assertNull(FloorDiv.floorDiv(null, 3L));
        assertNull(FloorDiv.floorDiv(19L, null));
        assertNull(FloorDiv.floorDiv(null, 3.1f));
        assertNull(FloorDiv.floorDiv(4.5f, null));
        assertNull(FloorDiv.floorDiv(null, 3.1));
        assertNull(FloorDiv.floorDiv(4.5, (Double) null));
        assertNull(FloorDiv.floorDiv(null, BigInteger.valueOf(3)));
        assertNull(FloorDiv.floorDiv(BigInteger.valueOf(19), null));
        assertNull(FloorDiv.floorDiv(null, BigDecimal.valueOf(3)));
        assertNull(FloorDiv.floorDiv(BigDecimal.valueOf(19), null));
    }

    @Test
    void differsFromQuotientWhenSignsDiffer() {
        // Truncation vs floor diverge when signs differ and dividend isn't a multiple of divisor.
        assertEquals(-3, Quotient.quotient(-10, 3));
        assertEquals(-4, FloorDiv.floorDiv(-10, 3));
        // But agree when signs match.
        assertEquals(3, Quotient.quotient(10, 3));
        assertEquals(3, FloorDiv.floorDiv(10, 3));
        // And agree on exact divisibility regardless of signs.
        assertEquals(-3, Quotient.quotient(-15, 5));
        assertEquals(-3, FloorDiv.floorDiv(-15, 5));
    }
}
