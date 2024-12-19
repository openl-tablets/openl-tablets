package org.openl.rules.util;

import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CovarTest {
    private static final double DELTA = 1e-9;

    private CovarTest() {

    }

    @Test
    public void testSampleCovariance() {
        assertNull(Covar.covarS((Number[]) null, null));
        assertNull(Covar.covarS(new Double[2], new Double[2]));
        assertNull(Covar.covarS(new Double[] { 1.0, null }, new Double[2]));
        assertNull(Covar.covarS(new Double[] { 1.0, 2.0 }, new Double[2]));

        assertNull(Covar.covarS(new Double[] { 9.5 }, new Double[] { 5.0 }));
        Assertions.assertEquals(24.333333333333332,
                Covar.covarS(new Double[] { 1.0, 10.0, 9.0 }, new Double[] { 1.0, 10.0, 9.0 }),
                DELTA);
        Assertions.assertEquals(32,
                Covar.covarS(new Double[] { 1.0, null, 9.0 }, new Double[] { 1.0, 10.0, 9.0 }),
                DELTA);
        Assertions.assertEquals(-20,
                Covar.covarS(new Double[] { -1.0, 10.0, 9.0 }, new Double[] { 1.0, -15.0, 9.0 }),
                DELTA);

        Assertions.assertEquals(0.5, Covar.covarS(new Long[] { 3L, 4L }, new Long[] { 3L, 4L }), DELTA);
        Assertions.assertEquals(7,
                Covar.covarS(new Byte[] { (byte) 3, (byte) 4, (byte) 8 }, new Byte[] { (byte) 3, (byte) 4, (byte) 8 }),
                DELTA);
        Assertions.assertEquals(7,
                Covar.covarS(new Short[] { (short) 3, (short) 4, (short) 8 },
                        new Short[] { (short) 3, (short) 4, (short) 8 }),
                DELTA);
        Assertions.assertEquals(7, Covar.covarS(new Integer[] { 3, 4, 8 }, new Integer[] { 3, 4, 8 }), DELTA);
        Assertions.assertEquals(0.0, Covar.covarS(new Integer[] { 10, 4, 8 }, new Integer[] { 3, 4, 8 }), DELTA);
        Assertions.assertEquals(7, Covar.covarS(new Long[] { 3L, 4L, 8L }, new Long[] { 3L, 4L, 8L }), DELTA);
        Assertions.assertEquals(7, Covar.covarS(new Float[] { 3f, 4f, 8f }, new Float[] { 3f, 4f, 8f }), DELTA);
        Assertions.assertEquals(7,
                Covar.covarS(new Byte[] { (byte) 3, (byte) 4, (byte) 8 },
                        new Short[] { (short) 3, (short) 4, (short) 8 }),
                DELTA);
        Assertions.assertEquals(new BigDecimal("0E-33"),
                Covar.covarS(new BigInteger[] { BigInteger.valueOf(10), BigInteger.valueOf(4), BigInteger.valueOf(8) },
                        new BigInteger[] { BigInteger.valueOf(3), BigInteger.valueOf(4), BigInteger.valueOf(8) }));
        Assertions.assertEquals(new BigDecimal("0.50"),
                Covar.covarS(new BigDecimal[] { BigDecimal.valueOf(3), BigDecimal.valueOf(4) },
                        new BigDecimal[] { BigDecimal.valueOf(3), BigDecimal.valueOf(4) }));

    }

    @Test
    public void testPopulationCovariance() {
        assertNull(Covar.covarP((Number[]) null, null));
        assertNull(Covar.covarP(new Double[2], new Double[2]));
        assertNull(Covar.covarP(new Double[] { 1.0, null }, new Double[] { null, null }));
        assertNull(Covar.covarP(new Double[] { 1.0, 2.0 }, new Double[] { null, null }));

        Assertions.assertEquals(0, Covar.covarP(new Double[] { 9.5 }, new Double[] { 5.0 }), DELTA);
        Assertions.assertEquals(16.22222222222222,
                Covar.covarP(new Double[] { 1.0, 10.0, 9.0 }, new Double[] { 1.0, 10.0, 9.0 }),
                DELTA);
        Assertions.assertEquals(16,
                Covar.covarP(new Double[] { 1.0, null, 9.0 }, new Double[] { 1.0, 10.0, 9.0 }),
                DELTA);
        Assertions.assertEquals(-13.333333333333334,
                Covar.covarP(new Double[] { -1.0, 10.0, 9.0 }, new Double[] { 1.0, -15.0, 9.0 }),
                DELTA);

        Assertions.assertEquals(0.25, Covar.covarP(new Long[] { 3L, 4L }, new Long[] { 3L, 4L }), DELTA);
        Assertions.assertEquals(4.666666666666667,
                Covar.covarP(new Byte[] { (byte) 3, (byte) 4, (byte) 8 }, new Byte[] { (byte) 3, (byte) 4, (byte) 8 }),
                DELTA);
        Assertions.assertEquals(4.666666666666667,
                Covar.covarP(new Short[] { (short) 3, (short) 4, (short) 8 },
                        new Short[] { (short) 3, (short) 4, (short) 8 }),
                DELTA);
        Assertions.assertEquals(4.666666666666667,
                Covar.covarP(new Integer[] { 3, 4, 8 }, new Integer[] { 3, 4, 8 }),
                DELTA);
        Assertions.assertEquals(4.666666666666667,
                Covar.covarP(new Long[] { 3L, 4L, 8L }, new Long[] { 3L, 4L, 8L }),
                DELTA);
        Assertions.assertEquals(4.666666666666667,
                Covar.covarP(new Float[] { 3f, 4f, 8f }, new Float[] { 3f, 4f, 8f }),
                DELTA);
        Assertions.assertEquals(4.666666666666667,
                Covar.covarP(new Byte[] { (byte) 3, (byte) 4, (byte) 8 },
                        new Short[] { (short) 3, (short) 4, (short) 8 }),
                DELTA);
        Assertions.assertEquals(new BigDecimal("0E-33"),
                Covar.covarP(new BigInteger[] { BigInteger.valueOf(10), BigInteger.valueOf(4), BigInteger.valueOf(8) },
                        new BigInteger[] { BigInteger.valueOf(3), BigInteger.valueOf(4), BigInteger.valueOf(8) }));
        Assertions.assertEquals(new BigDecimal("0.25"),
                Covar.covarP(new BigDecimal[] { BigDecimal.valueOf(3), BigDecimal.valueOf(4) },
                        new BigDecimal[] { BigDecimal.valueOf(3), BigDecimal.valueOf(4) }));

    }
}
