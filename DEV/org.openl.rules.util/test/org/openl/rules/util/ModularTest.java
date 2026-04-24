package org.openl.rules.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;

class ModularTest {

    @Test
    void modInt() {
        assertEquals(6, Modular.mod(19, 13));
        assertEquals(-7, Modular.mod(19, -13));
        assertEquals(7, Modular.mod(-19, 13));
        assertEquals(-6, Modular.mod(-19, -13));
        assertEquals(0, Modular.mod(19, 19));
        assertEquals(0, Modular.mod(-19, 19));
        assertEquals(0, Modular.mod(0, 19));
        assertEquals(1, Modular.mod(1, 3));
        assertEquals(2, Modular.mod(-1, 3));
    }

    @Test
    void modByteLarge() {
        // Boundary values never overflow the byte return because |remainder| < |divisor| <= 128.
        assertEquals((byte) 1, Modular.mod(Byte.MAX_VALUE, (byte) 7));
        assertEquals((byte) 5, Modular.mod(Byte.MIN_VALUE, (byte) 7));
        assertEquals((byte) -1, Modular.mod(Byte.MAX_VALUE, Byte.MIN_VALUE));
        assertEquals((byte) 0, Modular.mod(Byte.MIN_VALUE, (byte) -1));
        assertEquals((byte) 0, Modular.mod(Byte.MIN_VALUE, (byte) 1));
        assertEquals((byte) 0, Modular.mod(Byte.MIN_VALUE, Byte.MIN_VALUE));
    }

    @Test
    void modShortLarge() {
        assertEquals((short) 9, Modular.mod(Short.MAX_VALUE, (short) 11));
        assertEquals((short) 1, Modular.mod(Short.MIN_VALUE, (short) 11));
        assertEquals((short) -1, Modular.mod(Short.MAX_VALUE, Short.MIN_VALUE));
        assertEquals((short) 0, Modular.mod(Short.MIN_VALUE, (short) -1));
        assertEquals((short) 0, Modular.mod(Short.MIN_VALUE, (short) 1));
        assertEquals((short) 0, Modular.mod(Short.MIN_VALUE, Short.MIN_VALUE));
    }

    @Test
    void modFloatLarge() {
        // 1e7 is exactly representable in float (< 2^24).
        assertEquals(3.0f, Modular.mod(1.0e7f, 7.0f), 0.0f);
        assertEquals(-4.0f, Modular.mod(1.0e7f, -7.0f), 0.0f);
        assertEquals(4.0f, Modular.mod(-1.0e7f, 7.0f), 0.0f);
        assertEquals(-3.0f, Modular.mod(-1.0e7f, -7.0f), 0.0f);

        // Near-MAX magnitudes stay finite and obey the magnitude bound |result| < |divisor|.
        float r = Modular.mod(Float.MAX_VALUE, 7.0f);
        assertTrue(Float.isFinite(r));
        assertTrue(r >= 0.0f && r < 7.0f);

        r = Modular.mod(-Float.MAX_VALUE, 7.0f);
        assertTrue(Float.isFinite(r));
        assertTrue(r >= 0.0f && r < 7.0f);
    }

    @Test
    void modIntOverflowEdgeCase() {
        // Integer.MIN_VALUE % -1 returns 0 in Java (avoids the overflow of the division).
        assertEquals(0, Modular.mod(Integer.MIN_VALUE, -1));
        assertEquals(0, Modular.mod(Integer.MIN_VALUE, 1));
    }

    @Test
    void modIntByZero() {
        // x mod 0 == x (does not throw)
        assertEquals(5, Modular.mod(5, 0));
        assertEquals(-5, Modular.mod(-5, 0));
        assertEquals(0, Modular.mod(0, 0));
    }

    @Test
    void modLong() {
        assertEquals(6L, Modular.mod(19L, 13L));
        assertEquals(-7L, Modular.mod(19L, -13L));
        assertEquals(7L, Modular.mod(-19L, 13L));
        assertEquals(-6L, Modular.mod(-19L, -13L));
        assertEquals(0L, Modular.mod(19L, 19L));
        assertEquals(0L, Modular.mod(0L, 19L));
        assertEquals(0L, Modular.mod(Long.MIN_VALUE, -1L));
    }

    @Test
    void modLongByZero() {
        assertEquals(5L, Modular.mod(5L, 0L));
        assertEquals(-5L, Modular.mod(-5L, 0L));
    }

    @Test
    void modByte() {
        assertEquals((byte) 6, Modular.mod((byte) 19, (byte) 13));
        assertEquals((byte) -7, Modular.mod((byte) 19, (byte) -13));
        assertEquals((byte) 7, Modular.mod((byte) -19, (byte) 13));
        assertEquals((byte) -6, Modular.mod((byte) -19, (byte) -13));
        assertEquals((byte) 0, Modular.mod((byte) 0, (byte) 13));
    }

    @Test
    void modShort() {
        assertEquals((short) 6, Modular.mod((short) 19, (short) 13));
        assertEquals((short) -7, Modular.mod((short) 19, (short) -13));
        assertEquals((short) 7, Modular.mod((short) -19, (short) 13));
        assertEquals((short) -6, Modular.mod((short) -19, (short) -13));
        assertEquals((short) 0, Modular.mod((short) 0, (short) 13));
    }

    @Test
    void modFloat() {
        assertEquals(1.47f, Modular.mod(3.22f, 1.75f), 0.001f);
        assertEquals(-0.28f, Modular.mod(3.22f, -1.75f), 0.001f);
        assertEquals(0.28f, Modular.mod(-3.22f, 1.75f), 0.001f);
        assertEquals(-1.47f, Modular.mod(-3.22f, -1.75f), 0.001f);
        assertEquals(0.0f, Modular.mod(3.5f, 1.75f), 0.0f);
        assertEquals(0.0f, Modular.mod(0.0f, 1.75f), 0.0f);
    }

    @Test
    void modFloatByZero() {
        assertEquals(3.22f, Modular.mod(3.22f, 0.0f), 0.0f);
        assertEquals(3.22f, Modular.mod(3.22f, -0.0f), 0.0f);
        assertEquals(-3.22f, Modular.mod(-3.22f, 0.0f), 0.0f);
    }

    @Test
    void modFloatNanAndInfinity() {
        // NaN propagates (IEEE 754).
        assertTrue(Float.isNaN(Modular.mod(Float.NaN, 3.0f)));
        assertTrue(Float.isNaN(Modular.mod(3.0f, Float.NaN)));
        // finite % ±Inf = finite (no floor-mod adjustment when divisor is infinite).
        assertEquals(3.0f, Modular.mod(3.0f, Float.POSITIVE_INFINITY), 0.0f);
        assertEquals(-3.0f, Modular.mod(-3.0f, Float.POSITIVE_INFINITY), 0.0f);
        // ±Inf % finite = NaN.
        assertTrue(Float.isNaN(Modular.mod(Float.POSITIVE_INFINITY, 3.0f)));
        assertTrue(Float.isNaN(Modular.mod(Float.NEGATIVE_INFINITY, 3.0f)));
    }

    @Test
    void modDouble() {
        assertEquals(1.47, Modular.mod(3.22, 1.75), 0.001);
        assertEquals(-0.28, Modular.mod(3.22, -1.75), 0.001);
        assertEquals(0.28, Modular.mod(-3.22, 1.75), 0.001);
        assertEquals(-1.47, Modular.mod(-3.22, -1.75), 0.001);
        assertEquals(0.0, Modular.mod(3.5, 1.75), 0.0);
        assertEquals(0.0, Modular.mod(0.0, 1.75), 0.0);
    }

    @Test
    void modDoubleByZero() {
        assertEquals(3.22, Modular.mod(3.22, 0.0), 0.0);
        assertEquals(3.22, Modular.mod(3.22, -0.0), 0.0);
        assertEquals(-3.22, Modular.mod(-3.22, 0.0), 0.0);
    }

    @Test
    void modDoubleNanAndInfinity() {
        assertTrue(Double.isNaN(Modular.mod(Double.NaN, 3.0)));
        assertTrue(Double.isNaN(Modular.mod(3.0, Double.NaN)));
        assertEquals(3.0, Modular.mod(3.0, Double.POSITIVE_INFINITY), 0.0);
        assertEquals(-3.0, Modular.mod(-3.0, Double.POSITIVE_INFINITY), 0.0);
        assertTrue(Double.isNaN(Modular.mod(Double.POSITIVE_INFINITY, 3.0)));
        assertTrue(Double.isNaN(Modular.mod(Double.NEGATIVE_INFINITY, 3.0)));
    }

    @Test
    void modBigInteger() {
        assertEquals(BigInteger.valueOf(6), Modular.mod(BigInteger.valueOf(19), BigInteger.valueOf(13)));
        assertEquals(BigInteger.valueOf(-7), Modular.mod(BigInteger.valueOf(19), BigInteger.valueOf(-13)));
        assertEquals(BigInteger.valueOf(7), Modular.mod(BigInteger.valueOf(-19), BigInteger.valueOf(13)));
        assertEquals(BigInteger.valueOf(-6), Modular.mod(BigInteger.valueOf(-19), BigInteger.valueOf(-13)));
        assertEquals(BigInteger.ZERO, Modular.mod(BigInteger.valueOf(19), BigInteger.valueOf(19)));
        assertEquals(BigInteger.ZERO, Modular.mod(BigInteger.ZERO, BigInteger.valueOf(19)));
    }

    @Test
    void modBigIntegerLarge() {
        // quotient far exceeds Long.MAX_VALUE — verifies no silent long-truncation.
        BigInteger dividend = new BigInteger("203000745502000030060144252100");
        assertEquals(BigInteger.valueOf(59), Modular.mod(dividend, BigInteger.valueOf(97)));
        assertEquals(BigInteger.valueOf(-38), Modular.mod(dividend, BigInteger.valueOf(-97)));
        assertEquals(BigInteger.valueOf(38), Modular.mod(dividend.negate(), BigInteger.valueOf(97)));
        assertEquals(BigInteger.valueOf(-59), Modular.mod(dividend.negate(), BigInteger.valueOf(-97)));
    }

    @Test
    void modBigIntegerByZero() {
        assertEquals(BigInteger.valueOf(19), Modular.mod(BigInteger.valueOf(19), BigInteger.ZERO));
        assertEquals(BigInteger.valueOf(-19), Modular.mod(BigInteger.valueOf(-19), BigInteger.ZERO));
    }

    @Test
    void modBigIntegerNull() {
        assertNull(Modular.mod((BigInteger) null, BigInteger.valueOf(13)));
        assertNull(Modular.mod(BigInteger.valueOf(13), (BigInteger) null));
        assertNull(Modular.mod((BigInteger) null, (BigInteger) null));
    }

    @Test
    void modBigDecimal() {
        assertEquals(BigDecimal.valueOf(1.47), Modular.mod(BigDecimal.valueOf(3.22), BigDecimal.valueOf(1.75)));
        assertEquals(BigDecimal.valueOf(-0.28), Modular.mod(BigDecimal.valueOf(3.22), BigDecimal.valueOf(-1.75)));
        assertEquals(BigDecimal.valueOf(0.28), Modular.mod(BigDecimal.valueOf(-3.22), BigDecimal.valueOf(1.75)));
        assertEquals(BigDecimal.valueOf(-1.47), Modular.mod(BigDecimal.valueOf(-3.22), BigDecimal.valueOf(-1.75)));
        assertEquals(BigDecimal.ZERO, Modular.mod(BigDecimal.valueOf(3.5), BigDecimal.valueOf(1.75)));
        assertEquals(BigDecimal.ZERO, Modular.mod(BigDecimal.ZERO, BigDecimal.valueOf(1.75)));
    }

    @Test
    void modBigDecimalLarge() {
        BigDecimal dividend = new BigDecimal("203000745502000030060144252100");
        assertEquals(BigDecimal.valueOf(59), Modular.mod(dividend, BigDecimal.valueOf(97)));
        assertEquals(BigDecimal.valueOf(-38), Modular.mod(dividend, BigDecimal.valueOf(-97)));
        assertEquals(BigDecimal.valueOf(38), Modular.mod(dividend.negate(), BigDecimal.valueOf(97)));
        assertEquals(BigDecimal.valueOf(-59), Modular.mod(dividend.negate(), BigDecimal.valueOf(-97)));
    }

    @Test
    void modBigDecimalByZero() {
        assertEquals(BigDecimal.valueOf(19), Modular.mod(BigDecimal.valueOf(19), BigDecimal.ZERO));
        assertEquals(BigDecimal.valueOf(-19), Modular.mod(BigDecimal.valueOf(-19), BigDecimal.ZERO));
    }

    @Test
    void modBigDecimalNull() {
        assertNull(Modular.mod(null, BigDecimal.valueOf(13)));
        assertNull(Modular.mod(BigDecimal.valueOf(13), null));
        assertNull(Modular.mod(null, (BigDecimal) null));
    }

    @Test
    void modByteBoxed() {
        assertEquals((byte) 6, Modular.mod((byte) 19, (byte) 13));
        assertEquals((byte) -7, Modular.mod((byte) 19, (byte) -13));
        assertNull(Modular.mod(null, (byte) 13));
        assertNull(Modular.mod((byte) 13, null));
        assertNull(Modular.mod(null, (Byte) null));
    }

    @Test
    void modShortBoxed() {
        assertEquals((short) 6, Modular.mod((short) 19, (short) 13));
        assertEquals((short) -7, Modular.mod((short) 19, (short) -13));
        assertNull(Modular.mod(null, (short) 13));
        assertNull(Modular.mod((short) 13, null));
        assertNull(Modular.mod(null, (Short) null));
    }

    @Test
    void modIntegerBoxed() {
        assertEquals(6, Modular.mod(19, 13));
        assertEquals(-7, Modular.mod(19, -13));
        assertNull(Modular.mod(null, 13));
        assertNull(Modular.mod(13, null));
        assertNull(Modular.mod(null, (Integer) null));
    }

    @Test
    void modLongBoxed() {
        assertEquals(6L, Modular.mod(19L, 13L));
        assertEquals(-7L, Modular.mod(19L, -13L));
        assertNull(Modular.mod(null, 13L));
        assertNull(Modular.mod(13L, null));
        assertNull(Modular.mod(null, (Long) null));
    }

    @Test
    void modFloatBoxed() {
        assertEquals(1.47f, Modular.mod(3.22f, 1.75f), 0.001f);
        assertEquals(-0.28f, Modular.mod(3.22f, -1.75f), 0.001f);
        assertNull(Modular.mod(null, 1.75f));
        assertNull(Modular.mod(1.75f, null));
        assertNull(Modular.mod(null, (Float) null));
    }

    @Test
    void modDoubleBoxed() {
        assertEquals(1.47, Modular.mod(3.22, 1.75), 0.001);
        assertEquals(-0.28, Modular.mod(3.22, -1.75), 0.001);
        assertNull(Modular.mod(null, 1.75));
        assertNull(Modular.mod(1.75, null));
        assertNull(Modular.mod(null, (Double) null));
    }
}
