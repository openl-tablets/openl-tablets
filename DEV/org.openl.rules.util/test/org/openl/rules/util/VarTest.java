package org.openl.rules.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import static org.openl.rules.util.Var.varP;
import static org.openl.rules.util.Var.varS;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;

public class VarTest {
    private static final double DELTA = 1e-9;

    @Test
    public void testPopulationVariance() {
        assertNull(varP((Number) null));
        assertNull(varP(new Double[2]));

        assertEquals(16.22222222222222, varP(1, 10, 9), DELTA);
        assertEquals(0, varP(9.5), DELTA);
        assertEquals(9.0, varP(8, null, 2), DELTA);
        assertEquals(60.666666666666664, varP(-10.0, 6.0, 7.0), DELTA);

        assertEquals(4.666666666666667, varP((byte) 3, (byte) 4, (byte) 8), DELTA);
        assertEquals(4.666666666666667, varP((short) 3, (short) 4, (short) 8), DELTA);
        assertEquals(4.666666666666667, varP(3, 4, 8), DELTA);
        assertEquals(4.666666666666667, varP(3L, 4L, 8L), DELTA);
        assertEquals(4.666666666666667, varP(3f, 4f, 8f), DELTA);
        assertEquals(4.666666666666667, varP(3d, 4d, 8d), DELTA);
        assertEquals(4.666666666666667, varP((byte) 3, (short) 4, 8), DELTA);
        assertEquals(new BigDecimal("6.222222222222222222222222222222222"),
                varP(BigInteger.valueOf(10), BigInteger.valueOf(4), BigInteger.valueOf(8)));
        assertEquals(new BigDecimal("0.25"), varP(BigDecimal.valueOf(3), BigDecimal.valueOf(4)));
        assertEquals(new BigDecimal("4.222222222222222222222222222222222"),
                varP(BigInteger.valueOf(3), BigInteger.valueOf(5), BigInteger.valueOf(8)));

    }

    @Test
    public void testSampleVariance() {
        assertNull(varS((Number) null));
        assertNull(varS(new Double[2]));

        assertEquals(24.333333333333332, varS(1, 10, 9), DELTA);
        assertNull(varS(9.5));
        assertEquals(18.0, varS(8, null, 2), DELTA);
        assertEquals(91.0, varS(-10.0, 6.0, 7.0), DELTA);

        assertEquals(7.0, varS((byte) 3, (byte) 4, (byte) 8), DELTA);
        assertEquals(7.0, varS((short) 3, (short) 4, (short) 8), DELTA);
        assertEquals(7.0, varS(3, 4, 8), DELTA);
        assertEquals(7.0, varS(3L, 4L, 8L), DELTA);
        assertEquals(7.0, varS(3f, 4f, 8f), DELTA);
        assertEquals(7.0, varS(3d, 4d, 8d), DELTA);
        assertEquals(7.0, varS((byte) 3, (short) 4, 8), DELTA);
        assertEquals(new BigDecimal("9.333333333333333333333333333333333"),
                varS(BigInteger.valueOf(10), BigInteger.valueOf(4), BigInteger.valueOf(8)));
        BigDecimal[] testArray = new BigDecimal[2];
        testArray[0] = new BigDecimal("3");
        testArray[1] = new BigDecimal("4");
        assertEquals(new BigDecimal("0.50"), varS(testArray));
        assertEquals(new BigDecimal("0.50"), varS(BigDecimal.valueOf(3), BigDecimal.valueOf(4)));
        assertEquals(new BigDecimal("6.333333333333333333333333333333333"),
                varS(BigInteger.valueOf(3), BigInteger.valueOf(5), BigInteger.valueOf(8)));

    }
}
