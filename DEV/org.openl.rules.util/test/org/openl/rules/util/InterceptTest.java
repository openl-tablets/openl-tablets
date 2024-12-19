package org.openl.rules.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import static org.openl.rules.util.Intercept.intercept;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;

public class InterceptTest {

    @Test
    public void testIntercept() {
        assertNull(intercept((Number[]) null, null));
        assertNull(intercept(new Double[2], new Double[2]));
        assertNull(intercept(new Double[]{1.0, null}, new Double[]{null, null}));
        assertNull(intercept(new Double[]{1.0, 2.0}, new Double[]{null, null}));

        assertNull(intercept(new Double[]{9.5}, new Double[]{5.0}));
        assertEquals(2.846153846153846, intercept(new Double[]{3.0, 4.0, 8.0}, new Double[]{3.0, 10.0, 8.0}));
        assertNull(intercept(new Double[]{3.0, 3.0, 3.0}, new Double[]{3.0, 3.0, 3.0}));
        assertEquals(0.0, intercept(new Double[]{3.0, null, 8.0}, new Double[]{3.0, 10.0, 8.0}));
        assertEquals(2.976833976833977, intercept(new Double[]{-3.0, 4.0, 8.0}, new Double[]{3.0, -10.0, 8.0}));

        assertEquals(0.0, intercept(new Long[]{3L, 4L}, new Long[]{3L, 4L}));

        assertEquals(2.846153846153846, intercept(new Byte[]{(byte) 3, (byte) 4, (byte) 8}, new Byte[]{(byte) 3, (byte) 10, (byte) 8}));
        assertEquals(2.846153846153846, intercept(new Short[]{(short) 3, (short) 4, (short) 8}, new Short[]{(short) 3, (short) 10, (short) 8}));
        assertEquals(2.846153846153846, intercept(new Integer[]{3, 4, 8}, new Integer[]{3, 10, 8}));
        assertEquals(2.846153846153846, intercept(new Long[]{3L, 4L, 8L}, new Long[]{3L, 10L, 8L}));
        assertEquals(2.846153846153846, intercept(new Float[]{3f, 4f, 8f}, new Float[]{3f, 10f, 8f}));
        assertEquals(2.846153846153846, intercept(new Byte[]{(byte) 3, (byte) 4, (byte) 8}, new Short[]{(short) 3, (short) 10, (short) 8}));
        assertEquals(new BigDecimal("2.8461538461538461538461538461538461"), intercept(new BigInteger[]{BigInteger.valueOf(3), BigInteger.valueOf(4), BigInteger.valueOf(8)}, new BigInteger[]{BigInteger.valueOf(3), BigInteger.valueOf(10), BigInteger.valueOf(8)}));
        assertEquals(new BigDecimal("2.8461538461538461538461538461538461"), intercept(new BigDecimal[]{BigDecimal.valueOf(3), BigDecimal.valueOf(4), BigDecimal.valueOf(8)}, new BigDecimal[]{BigDecimal.valueOf(3), BigDecimal.valueOf(10), BigDecimal.valueOf(8)}));

    }
}
