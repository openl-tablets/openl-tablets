package org.openl.rules.util;

import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class CovarTest {

    @Test
    void testSampleCovariance() {
        assertNull(Covar.covarS((Number[]) null, null));
        assertNull(Covar.covarS(new Double[2], new Double[2]));
        assertNull(Covar.covarS(new Double[]{1.0, null}, new Double[2]));
        assertNull(Covar.covarS(new Double[]{1.0, 2.0}, new Double[2]));

        assertNull(Covar.covarS(new Double[]{9.5}, new Double[]{5.0}));
        Assertions.assertEquals(4.0, Covar.covarS(new Double[]{3.0, 4.0, 8.0}, new Double[]{3.0, 10.0, 8.0}));
        Assertions.assertEquals(12.5, Covar.covarS(new Double[]{3.0, null, 8.0}, new Double[]{3.0, 10.0, 8.0}));
        Assertions.assertEquals(6.0, Covar.covarS(new Double[]{-3.0, 4.0, 8.0}, new Double[]{3.0, -10.0, 8.0}));

        Assertions.assertEquals(0.5, Covar.covarS(new Long[]{3L, 4L}, new Long[]{3L, 4L}));
        Assertions.assertEquals(4.0, Covar.covarS(new Byte[]{(byte) 3, (byte) 4, (byte) 8}, new Byte[]{(byte) 3, (byte) 10, (byte) 8}));
        Assertions.assertEquals(4.0, Covar.covarS(new Short[]{(short) 3, (short) 4, (short) 8}, new Short[]{(short) 3, (short) 10, (short) 8}));
        Assertions.assertEquals(4.0, Covar.covarS(new Integer[]{3, 4, 8}, new Integer[]{3, 10, 8}));
        Assertions.assertEquals(4.0, Covar.covarS(new Integer[]{3, 4, 8}, new Integer[]{3, 10, 8}));
        Assertions.assertEquals(4.0, Covar.covarS(new Long[]{3L, 4L, 8L}, new Long[]{3L, 10L, 8L}));
        Assertions.assertEquals(4.0f, Covar.covarS(new Float[]{3f, 4f, 8f}, new Float[]{3f, 10f, 8f}));
        Assertions.assertEquals(4.0, Covar.covarS(new Byte[]{(byte) 3, (byte) 4, (byte) 8}, new Short[]{(short) 3, (short) 10, (short) 8}));
        Assertions.assertEquals(new BigDecimal("4"), Covar.covarS(new BigInteger[]{BigInteger.valueOf(3), BigInteger.valueOf(4), BigInteger.valueOf(8)}, new BigInteger[]{BigInteger.valueOf(3), BigInteger.valueOf(10), BigInteger.valueOf(8)}));
        Assertions.assertEquals(new BigDecimal("4"), Covar.covarS(new BigDecimal[]{BigDecimal.valueOf(3), BigDecimal.valueOf(4), BigDecimal.valueOf(8)}, new BigDecimal[]{BigDecimal.valueOf(3), BigDecimal.valueOf(10), BigDecimal.valueOf(8)}));

    }

    @Test
    void testPopulationCovariance() {
        assertNull(Covar.covarP((Number[]) null, null));
        assertNull(Covar.covarP(new Double[2], new Double[2]));
        assertNull(Covar.covarP(new Double[]{1.0, null}, new Double[]{null, null}));
        assertNull(Covar.covarP(new Double[]{1.0, 2.0}, new Double[]{null, null}));

        Assertions.assertEquals(0, Covar.covarP(new Double[]{9.5}, new Double[]{5.0}));
        Assertions.assertEquals(2.6666666666666665, Covar.covarP(new Double[]{3.0, 4.0, 8.0}, new Double[]{3.0, 10.0, 8.0}));
        Assertions.assertEquals(6.25, Covar.covarP(new Double[]{3.0, null, 8.0}, new Double[]{3.0, 10.0, 8.0}));
        Assertions.assertEquals(4.0, Covar.covarP(new Double[]{-3.0, 4.0, 8.0}, new Double[]{3.0, -10.0, 8.0}));

        Assertions.assertEquals(0.25, Covar.covarP(new Long[]{3L, 4L}, new Long[]{3L, 4L}));
        Assertions.assertEquals(2.6666666666666665, Covar.covarP(new Byte[]{(byte) 3, (byte) 4, (byte) 8}, new Byte[]{(byte) 3, (byte) 10, (byte) 8}));
        Assertions.assertEquals(2.6666666666666665, Covar.covarP(new Short[]{(short) 3, (short) 4, (short) 8}, new Short[]{(short) 3, (short) 10, (short) 8}));
        Assertions.assertEquals(2.6666666666666665, Covar.covarP(new Integer[]{3, 4, 8}, new Integer[]{3, 10, 8}));
        Assertions.assertEquals(2.6666666666666665, Covar.covarP(new Long[]{3L, 4L, 8L}, new Long[]{3L, 10L, 8L}));
        Assertions.assertEquals(2.6666666666666665f, Covar.covarP(new Float[]{3f, 4f, 8f}, new Float[]{3f, 10f, 8f}));
        Assertions.assertEquals(2.6666666666666665, Covar.covarP(new Byte[]{(byte) 3, (byte) 4, (byte) 8}, new Short[]{(short) 3, (short) 10, (short) 8}));
        Assertions.assertEquals(new BigDecimal("2.666666666666666666666666666666667"), Covar.covarP(new BigInteger[]{BigInteger.valueOf(3), BigInteger.valueOf(4), BigInteger.valueOf(8)}, new BigInteger[]{BigInteger.valueOf(3), BigInteger.valueOf(10), BigInteger.valueOf(8)}));
        Assertions.assertEquals(new BigDecimal("2.666666666666666666666666666666667"), Covar.covarP(new BigDecimal[]{BigDecimal.valueOf(3), BigDecimal.valueOf(4), BigDecimal.valueOf(8)}, new BigDecimal[]{BigDecimal.valueOf(3), BigDecimal.valueOf(10), BigDecimal.valueOf(8)}));

    }
}
