package org.openl.rules.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import static org.openl.rules.util.Forecast.forecast;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;

public class ForecastTest {

    @Test
    public void testForecast() {
        assertNull(forecast((Number) null, null, null));
        assertNull(forecast(null, new Double[2], new Double[2]));
        assertNull(forecast(1, new Double[]{1.0, null}, new Double[]{null, null}));
        assertNull(forecast(1, new Double[]{1.0, 2.0}, new Double[]{null, null}));

        assertNull(forecast(1, new Double[]{9.5}, new Double[]{5.0}));
        assertEquals(3.1538461538461533, forecast(1, new Double[]{3.0, 4.0, 8.0}, new Double[]{3.0, 10.0, 8.0}));
        assertNull(forecast(1, new Double[]{3.0, 3.0, 3.0}, new Double[]{3.0, 3.0, 3.0}));
        assertEquals(1.0, forecast(1, new Double[]{3.0, null, 8.0}, new Double[]{3.0, 10.0, 8.0}));
        assertEquals(3.0463320463320462, forecast(1, new Double[]{-3.0, 4.0, 8.0}, new Double[]{3.0, -10.0, 8.0}));

        assertEquals(1.0, forecast(1, new Long[]{3L, 4L}, new Long[]{3L, 4L}));

        assertEquals(3.1538461538461533, forecast(1, new Byte[]{(byte) 3, (byte) 4, (byte) 8}, new Byte[]{(byte) 3, (byte) 10, (byte) 8}));
        assertEquals(3.1538461538461533, forecast(1, new Short[]{(short) 3, (short) 4, (short) 8}, new Short[]{(short) 3, (short) 10, (short) 8}));
        assertEquals(3.1538461538461533, forecast(1, new Integer[]{3, 4, 8}, new Integer[]{3, 10, 8}));
        assertEquals(3.1538461538461533, forecast(1, new Long[]{3L, 4L, 8L}, new Long[]{3L, 10L, 8L}));
        assertEquals(3.1538461538461533, forecast(1, new Float[]{3f, 4f, 8f}, new Float[]{3f, 10f, 8f}));
        assertEquals(3.1538461538461533, forecast(1, new Byte[]{(byte) 3, (byte) 4, (byte) 8}, new Short[]{(short) 3, (short) 10, (short) 8}));
        assertEquals(new BigDecimal("3.1538461538461538461538461538461538"), forecast(new BigInteger("1"), new BigInteger[]{BigInteger.valueOf(3), BigInteger.valueOf(4), BigInteger.valueOf(8)}, new BigInteger[]{BigInteger.valueOf(3), BigInteger.valueOf(10), BigInteger.valueOf(8)}));
        assertEquals(new BigDecimal("3.1538461538461538461538461538461538"), forecast(new BigDecimal("1"), new BigDecimal[]{BigDecimal.valueOf(3), BigDecimal.valueOf(4), BigDecimal.valueOf(8)}, new BigDecimal[]{BigDecimal.valueOf(3), BigDecimal.valueOf(10), BigDecimal.valueOf(8)}));

    }
}
