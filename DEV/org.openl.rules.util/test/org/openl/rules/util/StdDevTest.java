package org.openl.rules.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import static org.openl.rules.util.StdDev.stdevP;
import static org.openl.rules.util.StdDev.stdevS;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;

public class StdDevTest {

    @Test
    public void testStandardPopulationDeviation() {
        assertNull(stdevP((Number) null));
        assertNull(stdevP(new Double[2]));

        assertEquals(4.0276819911981905, stdevP(1, 10, 9));
        assertEquals(0, stdevP(9.5));
        assertEquals(3, stdevP(8, null, 2));
        assertEquals(7.788880963698615, stdevP(-10.0, 6.0, 7.0));

        assertEquals(2.160246899469287, stdevP((byte) 3, (byte) 4, (byte) 8));
        assertEquals(2.160246899469287, stdevP((short) 3, (short) 4, (short) 8));
        assertEquals(2.160246899469287, stdevP(3, 4, 8));
        assertEquals(2.160246899469287, stdevP(3L, 4L, 8L));
        assertEquals(2.160246899469287, stdevP(3f, 4f, 8f));
        assertEquals(2.160246899469287, stdevP(3d, 4d, 8d));
        assertEquals(2.160246899469287, stdevP((byte) 3, (short) 4, 8));
        assertEquals(new BigDecimal("4.027681991198190689427613088935952"), stdevP(BigInteger.valueOf(1), BigInteger.valueOf(10), BigInteger.valueOf(9)));
        assertEquals(new BigDecimal("2.160246899469286743655322478695999"), stdevP(BigDecimal.valueOf(3), BigDecimal.valueOf(4), BigDecimal.valueOf(8)));
        assertEquals(new BigDecimal("2.160246899469286743655322478695999"), stdevP(BigInteger.valueOf(3), BigInteger.valueOf(4), BigInteger.valueOf(8)));

    }

    @Test
    public void testSampleStandardDeviation() {
        assertNull(stdevS((Number) null));
        assertNull(stdevS(new Double[2]));

        assertEquals(4.932882862316247, stdevS(1, 10, 9));
        assertNull(stdevS(9.5));
        assertEquals(4.242640687119285, stdevS(8, null, 2));
        assertEquals(9.539392014169456, stdevS(-10.0, 6.0, 7.0));

        assertEquals(2.6457513110645907, stdevS((byte) 3, (byte) 4, (byte) 8));
        assertEquals(2.6457513110645907, stdevS((short) 3, (short) 4, (short) 8));
        assertEquals(2.6457513110645907, stdevS(3, 4, 8));
        assertEquals(2.6457513110645907, stdevS(3L, 4L, 8L));
        assertEquals(2.6457513110645907, stdevS(3f, 4f, 8f));
        assertEquals(2.6457513110645907, stdevS(3d, 4d, 8d));
        assertEquals(2.6457513110645907, stdevS((byte) 3, (short) 4, 8));
        assertEquals(new BigDecimal("2.64575131106459059050161575363926"), stdevS(BigInteger.valueOf(3), BigInteger.valueOf(4), BigInteger.valueOf(8)));
        assertEquals(new BigDecimal("2.64575131106459059050161575363926"), stdevS(BigDecimal.valueOf(3), BigDecimal.valueOf(4), BigDecimal.valueOf(8)));
        assertEquals(new BigDecimal("2.64575131106459059050161575363926"), stdevS(BigInteger.valueOf(3), BigInteger.valueOf(4), BigInteger.valueOf(8)));

    }
}
