package org.openl.rules.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import static org.openl.rules.util.Var.varP;
import static org.openl.rules.util.Var.varS;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;

class VarTest {

    @Test
    void testPopulationVariance() {
        assertNull(varP((Number) null));
        assertNull(varP(new Double[2]));

        assertEquals(4.666666666666667, varP(3, 4, 8));
        assertEquals(0, varP(9.5));
        assertEquals(6.25, varP(3, null, 8));
        assertEquals(20.666666666666668, varP(-3.0, 4.0, 8.0));

        assertEquals(4.666666666666667, varP((byte) 3, (byte) 4, (byte) 8));
        assertEquals(4.666666666666667, varP((short) 3, (short) 4, (short) 8));
        assertEquals(4.666666666666667, varP(3L, 4L, 8L));
        assertEquals(4.666666666666667f, varP(3f, 4f, 8f));
        assertEquals(4.666666666666667, varP(3d, 4d, 8d));
        assertEquals(4.666666666666667, varP((byte) 3, (short) 4, 8));
        assertEquals(678.8399999999999, varP(1345, 1301, 1368, 1322, 1310, 1370, 1318, 1350, 1303, 1299));
        assertEquals(new BigDecimal("4.666666666666666666666666666666667"), varP(BigInteger.valueOf(3), BigInteger.valueOf(4), BigInteger.valueOf(8)));
        assertEquals(new BigDecimal("4.666666666666666666666666666666667"), varP(BigDecimal.valueOf(3), BigDecimal.valueOf(4), BigDecimal.valueOf(8)));
    }

    @Test
    void testSampleVariance() {
        assertNull(varS((Number) null));
        assertNull(varS(new Double[2]));

        assertEquals(7.0, varS(3, 4, 8));
        assertNull(varS(9.5));
        assertEquals(12.5, varS(3, null, 8));
        assertEquals(31.0, varS(-3.0, 4.0, 8.0));

        assertEquals(7.0, varS((byte) 3, (byte) 4, (byte) 8));
        assertEquals(7.0, varS((short) 3, (short) 4, (short) 8));
        assertEquals(7.0, varS(3, 4, 8));
        assertEquals(7.0, varS(3L, 4L, 8L));
        assertEquals(7.0f, varS(3f, 4f, 8f));
        assertEquals(7.0, varS(3d, 4d, 8d));
        assertEquals(7.0, varS((byte) 3, (short) 4, 8));
        assertEquals(754.2666666666665, varS(1345, 1301, 1368, 1322, 1310, 1370, 1318, 1350, 1303, 1299));
        assertEquals(new BigDecimal("7"), varS(BigInteger.valueOf(3), BigInteger.valueOf(4), BigInteger.valueOf(8)));
        assertEquals(new BigDecimal("7"), varS(BigDecimal.valueOf(3), BigDecimal.valueOf(4), BigDecimal.valueOf(8)));
    }

    @Test
    void testSingleNonNullValueReturnsNull() {
        // Test with single non-null value should return null for varS
        assertNull(varS(5, null));
        assertNull(varS(5.0));
        assertNull(varS(5L, null));
        assertNull(varS((short) 5, null));
        assertNull(varS(5f, null));
        assertNull(varS(BigDecimal.valueOf(5), null));
        assertNull(varS(BigInteger.valueOf(5), null));
    }

    @Test
    void testSingleNonNullValuePopulationVariance() {
        // Test with single non-null value should return 0 for varP
        assertEquals(0.0, varP(5, null));
        assertEquals(0.0, varP(5.0));
        assertEquals(0.0, varP(5L, null));
        assertEquals(0.0f, varP(5f, null));
        assertEquals(0.0, varP((short) 5, null));
        assertEquals(BigDecimal.ZERO, varP(BigDecimal.valueOf(5), null));
        assertEquals(BigDecimal.ZERO, varP(BigInteger.valueOf(5), null));
    }

    @Test
    void testIntegerArray() {
        // Test Integer array with single non-null value
        assertNull(varS(5, null));
        assertEquals(0.0, varP(5, null));
    }

    @Test
    void testFloatArray() {
        // Test Float array with single non-null value
        assertNull(varS(5.0f, null));
        assertEquals(0.0f, varP(5.0f, null));
    }

    @Test
    void testLongArray() {
        // Test Long array with single non-null value
        assertNull(varS(5L, null));
        assertEquals(0.0, varP(5L, null));
    }

    @Test
    void testShortArray() {
        // Test Short array with single non-null value
        assertNull(varS((short) 5, null));
        assertEquals(0.0, varP((short) 5, null));
    }
}
