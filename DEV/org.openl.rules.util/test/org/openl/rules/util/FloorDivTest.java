package org.openl.rules.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;

public class FloorDivTest {

    @Test
    public void canonicalExamples() {
        // floor(-10/3) = -4; trunc = -3. This is the defining difference from Quotient.
        assertEquals(-4, FloorDiv.floorDiv(-10, 3));
        assertEquals(2, FloorDiv.floorDiv(7, 3));
        assertEquals(-4, FloorDiv.floorDiv(7, -2));
        assertEquals(3, FloorDiv.floorDiv(-7, -2));
    }

    @Test
    public void floorDivInt() {
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
    public void floorDivIntByZero() {
        assertThrows(ArithmeticException.class, () -> FloorDiv.floorDiv(5, 0));
    }

    @Test
    public void floorDivLong() {
        assertEquals(3L, FloorDiv.floorDiv(19L, 5L));
        assertEquals(-4L, FloorDiv.floorDiv(-19L, 5L));
        assertEquals(-4L, FloorDiv.floorDiv(19L, -5L));
        assertEquals(3L, FloorDiv.floorDiv(-19L, -5L));
        // Large-magnitude: long precision preserved.
        assertEquals(3_000_000_000L, FloorDiv.floorDiv(9_000_000_000L, 3L));
    }

    @Test
    public void floorDivLongByZero() {
        assertThrows(ArithmeticException.class, () -> FloorDiv.floorDiv(5L, 0L));
    }

    @Test
    public void floorDivByte() {
        assertEquals(3, FloorDiv.floorDiv((byte) 19, (byte) 5));
        assertEquals(-4, FloorDiv.floorDiv((byte) 19, (byte) -5));
        assertEquals(-4, FloorDiv.floorDiv((byte) -19, (byte) 5));
    }

    @Test
    public void floorDivShort() {
        assertEquals(3, FloorDiv.floorDiv((short) 19, (short) 5));
        assertEquals(-4, FloorDiv.floorDiv((short) 19, (short) -5));
        assertEquals(-4, FloorDiv.floorDiv((short) -19, (short) 5));
    }

    @Test
    public void floorDivFloat() {
        assertEquals(1.0f, FloorDiv.floorDiv(4.5f, 3.1f), 0.0f);
        assertEquals(-2.0f, FloorDiv.floorDiv(4.5f, -3.1f), 0.0f);
        assertEquals(-2.0f, FloorDiv.floorDiv(-4.5f, 3.1f), 0.0f);
        assertEquals(1.0f, FloorDiv.floorDiv(-4.5f, -3.1f), 0.0f);
        assertEquals(0.0f, FloorDiv.floorDiv(0.0f, 3.1f), 0.0f);
    }

    @Test
    public void floorDivFloatByZero() {
        // IEEE 754: x/0 → ±Infinity, floor(±Inf) = ±Inf; 0/0 → NaN.
        assertEquals(Float.POSITIVE_INFINITY, FloorDiv.floorDiv(4.5f, 0.0f), 0.0f);
        assertEquals(Float.NEGATIVE_INFINITY, FloorDiv.floorDiv(-4.5f, 0.0f), 0.0f);
        assertTrue(Float.isNaN(FloorDiv.floorDiv(0.0f, 0.0f)));
    }

    @Test
    public void floorDivFloatNanAndInfinity() {
        assertTrue(Float.isNaN(FloorDiv.floorDiv(Float.NaN, 3.0f)));
        assertTrue(Float.isNaN(FloorDiv.floorDiv(3.0f, Float.NaN)));
        assertEquals(Float.POSITIVE_INFINITY, FloorDiv.floorDiv(Float.POSITIVE_INFINITY, 3.0f), 0.0f);
        assertEquals(Float.NEGATIVE_INFINITY, FloorDiv.floorDiv(Float.NEGATIVE_INFINITY, 3.0f), 0.0f);
        assertEquals(0.0f, FloorDiv.floorDiv(3.0f, Float.POSITIVE_INFINITY), 0.0f);
        // 3 / -Inf = -0.0f; Math.floor preserves the signed zero.
        assertEquals(0.0f, FloorDiv.floorDiv(3.0f, Float.NEGATIVE_INFINITY), 0.0f);
    }

    @Test
    public void floorDivDouble() {
        assertEquals(1.0, FloorDiv.floorDiv(4.5, 3.1), 0.0);
        assertEquals(-2.0, FloorDiv.floorDiv(4.5, -3.1), 0.0);
        assertEquals(-2.0, FloorDiv.floorDiv(-4.5, 3.1), 0.0);
        assertEquals(1.0, FloorDiv.floorDiv(-4.5, -3.1), 0.0);
    }

    @Test
    public void floorDivDoubleByZero() {
        assertEquals(Double.POSITIVE_INFINITY, FloorDiv.floorDiv(4.5, 0.0), 0.0);
        assertEquals(Double.NEGATIVE_INFINITY, FloorDiv.floorDiv(-4.5, 0.0), 0.0);
        assertTrue(Double.isNaN(FloorDiv.floorDiv(0.0, 0.0)));
    }

    @Test
    public void floorDivDoubleNanAndInfinity() {
        assertTrue(Double.isNaN(FloorDiv.floorDiv(Double.NaN, 3.0)));
        assertTrue(Double.isNaN(FloorDiv.floorDiv(3.0, Double.NaN)));
        assertEquals(Double.POSITIVE_INFINITY, FloorDiv.floorDiv(Double.POSITIVE_INFINITY, 3.0), 0.0);
        assertEquals(Double.NEGATIVE_INFINITY, FloorDiv.floorDiv(Double.NEGATIVE_INFINITY, 3.0), 0.0);
    }

    @Test
    public void floorDivBigInteger() {
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
    public void floorDivBigIntegerLarge() {
        // floor(-N/97) where N is > long range. For N positive, divisor positive: floor == trunc.
        BigInteger dividend = new BigInteger("203000745502000030060144252100");
        BigInteger expected = new BigInteger("2092791190742268351135507753");
        assertEquals(expected, FloorDiv.floorDiv(dividend, BigInteger.valueOf(97)));
        // Opposite-sign case: remainder is 59 (non-zero), so floor == trunc - 1.
        assertEquals(expected.negate().subtract(BigInteger.ONE),
                FloorDiv.floorDiv(dividend, BigInteger.valueOf(-97)));
    }

    @Test
    public void floorDivBigIntegerByZero() {
        assertThrows(ArithmeticException.class,
                () -> FloorDiv.floorDiv(BigInteger.valueOf(19), BigInteger.ZERO));
    }

    @Test
    public void floorDivBigDecimal() {
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
    public void floorDivBigDecimalByZero() {
        assertThrows(ArithmeticException.class,
                () -> FloorDiv.floorDiv(BigDecimal.valueOf(19), BigDecimal.ZERO));
    }

    @Test
    public void floorDivBoxedNulls() {
        assertNull(FloorDiv.floorDiv((Byte) null, (Byte) null));
        assertNull(FloorDiv.floorDiv((Byte) null, Byte.valueOf((byte) 3)));
        assertNull(FloorDiv.floorDiv(Byte.valueOf((byte) 19), (Byte) null));
        assertNull(FloorDiv.floorDiv((Short) null, Short.valueOf((short) 3)));
        assertNull(FloorDiv.floorDiv(Short.valueOf((short) 19), (Short) null));
        assertNull(FloorDiv.floorDiv((Integer) null, Integer.valueOf(3)));
        assertNull(FloorDiv.floorDiv(Integer.valueOf(19), (Integer) null));
        assertNull(FloorDiv.floorDiv((Long) null, Long.valueOf(3L)));
        assertNull(FloorDiv.floorDiv(Long.valueOf(19L), (Long) null));
        assertNull(FloorDiv.floorDiv((Float) null, Float.valueOf(3.1f)));
        assertNull(FloorDiv.floorDiv(Float.valueOf(4.5f), (Float) null));
        assertNull(FloorDiv.floorDiv((Double) null, Double.valueOf(3.1)));
        assertNull(FloorDiv.floorDiv(Double.valueOf(4.5), (Double) null));
        assertNull(FloorDiv.floorDiv((BigInteger) null, BigInteger.valueOf(3)));
        assertNull(FloorDiv.floorDiv(BigInteger.valueOf(19), (BigInteger) null));
        assertNull(FloorDiv.floorDiv((BigDecimal) null, BigDecimal.valueOf(3)));
        assertNull(FloorDiv.floorDiv(BigDecimal.valueOf(19), (BigDecimal) null));
    }

    @Test
    public void differsFromQuotientWhenSignsDiffer() {
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
