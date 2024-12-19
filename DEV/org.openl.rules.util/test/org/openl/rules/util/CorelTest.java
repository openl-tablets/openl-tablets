package org.openl.rules.util;

import static org.junit.jupiter.api.Assertions.assertNull;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CorelTest {
    private static final double DELTA = 1e-9;

    @Test
    public void testCorrelationCoefficient() {
        assertNull(Corel.correl((Number[]) null, null));
        assertNull(Corel.correl(new Double[2], new Double[2]));
        assertNull(Corel.correl(new Double[]{1.0, null}, new Double[]{null, null}));
        assertNull(Corel.correl(new Double[]{1.0, 2.0}, new Double[]{null, null}));

        assertNull(Corel.correl(new Double[]{9.5}, new Double[]{5.0}));
        Assertions.assertEquals(1,
                Corel.correl(new Double[]{1.0, 10.0, 9.0}, new Double[]{1.0, 10.0, 9.0}),
                DELTA);
        Assertions.assertEquals(0.9999999999999998,
                Corel.correl(new Double[]{1.0, null, 9.0}, new Double[]{1.0, 10.0, 9.0}),
                DELTA);
        Assertions.assertEquals(-0.2690610012503157,
                Corel.correl(new Double[]{-1.0, 10.0, 9.0}, new Double[]{1.0, -15.0, 9.0}),
                DELTA);

        Assertions.assertEquals(0.9999999999999998, Corel.correl(new Long[]{3L, 4L}, new Long[]{3L, 4L}), DELTA);
        Assertions.assertEquals(0.9999999999999999,
                Corel.correl(new Byte[]{(byte) 3, (byte) 4, (byte) 8}, new Byte[]{(byte) 3, (byte) 4, (byte) 8}),
                DELTA);
        Assertions.assertEquals(0.9999999999999999,
                Corel.correl(new Short[]{(short) 3, (short) 4, (short) 8},
                        new Short[]{(short) 3, (short) 4, (short) 8}),
                DELTA);
        Assertions.assertEquals(0.9999999999999999,
                Corel.correl(new Integer[]{3, 4, 8}, new Integer[]{3, 4, 8}),
                DELTA);
        Assertions.assertEquals(0.9999999999999999,
                Corel.correl(new Long[]{3L, 4L, 8L}, new Long[]{3L, 4L, 8L}),
                DELTA);
        Assertions.assertEquals(0.9999999999999999,
                Corel.correl(new Float[]{3f, 4f, 8f}, new Float[]{3f, 4f, 8f}),
                DELTA);
        Assertions.assertEquals(0.9999999999999999,
                Corel.correl(new Byte[]{(byte) 3, (byte) 4, (byte) 8},
                        new Short[]{(short) 3, (short) 4, (short) 8}),
                DELTA);
        Assertions.assertEquals(new BigDecimal("0E+32"),
                Corel.correl(new BigInteger[]{BigInteger.valueOf(10), BigInteger.valueOf(4), BigInteger.valueOf(8)},
                        new BigInteger[]{BigInteger.valueOf(3), BigInteger.valueOf(4), BigInteger.valueOf(8)}));
        Assertions.assertEquals(new BigDecimal("1.000000000000000000000000000000000"),
                Corel.correl(new BigDecimal[]{BigDecimal.valueOf(3), BigDecimal.valueOf(4)},
                        new BigDecimal[]{BigDecimal.valueOf(3), BigDecimal.valueOf(4)}));
    }

    @Test
    public void testRSQ() {
        assertNull(Corel.rsq((Number[]) null, null));
        assertNull(Corel.rsq(new Double[2], new Double[2]));
        assertNull(Corel.rsq(new Double[]{1.0, null}, new Double[]{null, null}));
        assertNull(Corel.rsq(new Double[]{1.0, 2.0}, new Double[]{null, null}));

        assertNull(Corel.rsq(new Double[]{9.5}, new Double[]{5.0}));
        Assertions.assertEquals(1, Corel.rsq(new Double[]{1.0, 10.0, 9.0}, new Double[]{1.0, 10.0, 9.0}), DELTA);
        Assertions.assertEquals(0.9999999999999996,
                Corel.rsq(new Double[]{1.0, null, 9.0}, new Double[]{1.0, 10.0, 9.0}),
                DELTA);
        Assertions.assertEquals(0.07239382239382237,
                Corel.rsq(new Double[]{-1.0, 10.0, 9.0}, new Double[]{1.0, -15.0, 9.0}),
                DELTA);

        Assertions.assertEquals(0.9999999999999996, Corel.rsq(new Long[]{3L, 4L}, new Long[]{3L, 4L}), DELTA);
        Assertions.assertEquals(0.9999999999999998,
                Corel.rsq(new Byte[]{(byte) 3, (byte) 4, (byte) 8}, new Byte[]{(byte) 3, (byte) 4, (byte) 8}),
                DELTA);
        Assertions.assertEquals(0.9999999999999998,
                Corel.rsq(new Short[]{(short) 3, (short) 4, (short) 8},
                        new Short[]{(short) 3, (short) 4, (short) 8}),
                DELTA);
        Assertions.assertEquals(0.9999999999999998,
                Corel.rsq(new Integer[]{3, 4, 8}, new Integer[]{3, 4, 8}),
                DELTA);
        Assertions.assertEquals(0.9999999999999998,
                Corel.rsq(new Long[]{3L, 4L, 8L}, new Long[]{3L, 4L, 8L}),
                DELTA);
        Assertions.assertEquals(0.9999999999999998,
                Corel.rsq(new Float[]{3f, 4f, 8f}, new Float[]{3f, 4f, 8f}),
                DELTA);
        Assertions.assertEquals(0.9999999999999998,
                Corel.rsq(new Byte[]{(byte) 3, (byte) 4, (byte) 8}, new Short[]{(short) 3, (short) 4, (short) 8}),
                DELTA);
        Assertions.assertEquals(new BigDecimal("0E+64"),
                Corel.rsq(new BigInteger[]{BigInteger.valueOf(10), BigInteger.valueOf(4), BigInteger.valueOf(8)},
                        new BigInteger[]{BigInteger.valueOf(3), BigInteger.valueOf(4), BigInteger.valueOf(8)}));
        Assertions.assertEquals(new BigDecimal("1.000000000000000000000000000000000"),
                Corel.rsq(new BigDecimal[]{BigDecimal.valueOf(3), BigDecimal.valueOf(4)},
                        new BigDecimal[]{BigDecimal.valueOf(3), BigDecimal.valueOf(4)}));
    }
}
