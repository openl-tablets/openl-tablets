package org.openl.rules.util;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.openl.rules.util.StdDev.stdevP;
import static org.openl.rules.util.StdDev.stdevS;

public class StdDevTest {
    private static final double DELTA = 1e-9;

    @Test
    public void testStandardPopulationDeviation() {
        assertNull(stdevP((Number) null));
        assertNull(stdevP(new Double[2]));

        assertEquals(4.0276819911981905, stdevP(1, 10, 9), DELTA);
        assertEquals(0, stdevP(9.5), DELTA);
        assertEquals(3, stdevP(8, null, 2), DELTA);
        assertEquals(7.788880963698615, stdevP(-10.0, 6.0, 7.0), DELTA);

        assertEquals(2.160246899469287, stdevP((byte) 3, (byte) 4, (byte) 8), DELTA);
        assertEquals(2.160246899469287, stdevP((short) 3, (short) 4, (short) 8), DELTA);
        assertEquals(2.160246899469287, stdevP(3, 4, 8), DELTA);
        assertEquals(2.160246899469287, stdevP(3L, 4L, 8L), DELTA);
        assertEquals(2.160246899469287, stdevP(3f, 4f, 8f), DELTA);
        assertEquals(2.160246899469287, stdevP(3d, 4d, 8d), DELTA);
        assertEquals(2.160246899469287, stdevP((byte) 3, (short) 4, 8), DELTA);
        assertEquals(new BigDecimal("2.494438257849294257055832488211033"),
                stdevP(BigInteger.valueOf(10), BigInteger.valueOf(4), BigInteger.valueOf(8)));
        assertEquals(new BigDecimal("0.5"), stdevP(BigDecimal.valueOf(3), BigDecimal.valueOf(4)));
        assertEquals(new BigDecimal("2.054804667656325483416730793818081"),
                stdevP(BigInteger.valueOf(3), BigInteger.valueOf(5), BigInteger.valueOf(8)));

    }

    @Test
    public void testSampleStandardDeviation() {
        assertNull(stdevS((Number) null));
        assertNull(stdevS(new Double[2]));

        assertEquals(4.932882862316247, stdevS(1, 10, 9), DELTA);
        assertNull(stdevS(9.5));
        assertEquals(4.242640687119285, stdevS(8, null, 2), DELTA);
        assertEquals(9.539392014169456, stdevS(-10.0, 6.0, 7.0), DELTA);

        assertEquals(2.6457513110645907, stdevS((byte) 3, (byte) 4, (byte) 8), DELTA);
        assertEquals(2.6457513110645907, stdevS((short) 3, (short) 4, (short) 8), DELTA);
        assertEquals(2.6457513110645907, stdevS(3, 4, 8), DELTA);
        assertEquals(2.6457513110645907, stdevS(3L, 4L, 8L), DELTA);
        assertEquals(2.6457513110645907, stdevS(3f, 4f, 8f), DELTA);
        assertEquals(2.6457513110645907, stdevS(3d, 4d, 8d), DELTA);
        assertEquals(2.6457513110645907, stdevS((byte) 3, (short) 4, 8), DELTA);
        assertEquals(new BigDecimal("3.055050463303893337725364795818672"),
                stdevS(BigInteger.valueOf(10), BigInteger.valueOf(4), BigInteger.valueOf(8)));
        assertEquals(new BigDecimal("0.707106781186547524400844362104849"),
                stdevS(BigDecimal.valueOf(3), BigDecimal.valueOf(4)));
        assertEquals(new BigDecimal("2.516611478423583232412228268982039"),
                stdevS(BigInteger.valueOf(3), BigInteger.valueOf(5), BigInteger.valueOf(8)));

    }
}
