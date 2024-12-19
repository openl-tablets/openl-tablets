package org.openl.rules.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import static org.openl.rules.util.Slope.slope;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;

public class SlopeTest {

    private static final double DELTA = 1e-9;

    @Test
    public void testSlope() {
        assertNull(slope((Number[]) null, null));
        assertNull(slope(new Double[2], new Double[2]));
        assertNull(slope(new Double[]{1.0, null}, new Double[]{null, null}));
        assertNull(slope(new Double[]{1.0, 2.0}, new Double[]{null, null}));

        assertNull(slope(new Double[]{9.5}, new Double[]{5.0}));
        assertEquals(1, slope(new Double[]{1.0, 10.0, 9.0}, new Double[]{1.0, 10.0, 9.0}), DELTA);
        assertEquals(1, slope(new Double[]{1.0, null, 9.0}, new Double[]{1.0, 10.0, 9.0}), DELTA);
        assertEquals(-0.13392857142857142,
                slope(new Double[]{-1.0, 10.0, 9.0}, new Double[]{1.0, -15.0, 9.0}),
                DELTA);

        assertEquals(1, slope(new Long[]{3L, 4L}, new Long[]{3L, 4L}), DELTA);
        assertEquals(1,
                slope(new Byte[]{(byte) 3, (byte) 4, (byte) 8}, new Byte[]{(byte) 3, (byte) 4, (byte) 8}),
                0);
        assertEquals(1,
                slope(new Short[]{(short) 3, (short) 4, (short) 8}, new Short[]{(short) 3, (short) 4, (short) 8}),
                DELTA);
        assertEquals(1, slope(new Integer[]{3, 4, 8}, new Integer[]{3, 4, 8}), DELTA);
        assertEquals(1, slope(new Long[]{3L, 4L, 8L}, new Long[]{3L, 4L, 8L}), DELTA);
        assertEquals(1, slope(new Float[]{3f, 4f, 8f}, new Float[]{3f, 4f, 8f}), DELTA);
        assertEquals(1,
                slope(new Byte[]{(byte) 3, (byte) 4, (byte) 8}, new Short[]{(short) 3, (short) 4, (short) 8}),
                DELTA);
        assertEquals(new BigDecimal("0E-33"),
                slope(new BigInteger[]{BigInteger.valueOf(10), BigInteger.valueOf(4), BigInteger.valueOf(8)},
                        new BigInteger[]{BigInteger.valueOf(3), BigInteger.valueOf(4), BigInteger.valueOf(8)}));
        assertEquals(new BigDecimal("1"),
                slope(new BigDecimal[]{BigDecimal.valueOf(3), BigDecimal.valueOf(4)},
                        new BigDecimal[]{BigDecimal.valueOf(3), BigDecimal.valueOf(4)}));
    }
}
