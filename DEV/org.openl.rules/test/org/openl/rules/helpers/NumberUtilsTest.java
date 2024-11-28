package org.openl.rules.helpers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

public class NumberUtilsTest {

    @Test
    public void testGetDoubleScale() {
        assertEquals(3, NumberUtils.getScale(12.678));
        assertEquals(1, NumberUtils.getScale(12.1));
        assertEquals(5, NumberUtils.getScale(12.67867));
        assertEquals(1, NumberUtils.getScale(0.2));

        assertEquals(0, NumberUtils.getScale(Double.NaN));
        assertEquals(0, NumberUtils.getScale(Double.NEGATIVE_INFINITY));
    }

    @Test
    public void testGetDoubleScaleNull() {
        assertThrows(NullPointerException.class, () -> {
            NumberUtils.getScale(null);
        });
    }

    @Test
    public void testGetNumberScale() {
        assertEquals(3, NumberUtils.getScale(Double.valueOf(12.678)));
        assertEquals(0, NumberUtils.getScale(Integer.valueOf(12)));
        assertEquals(0, NumberUtils.getScale(Integer.valueOf(0)));

        assertEquals(0, NumberUtils.getScale((Number) Double.NaN));
        assertEquals(0, NumberUtils.getScale((Number) Double.NEGATIVE_INFINITY));
    }

    @Test
    public void testGetNumberScaleNull() {
        assertThrows(NullPointerException.class, () -> {
            NumberUtils.getScale((Number) null);
        });
    }

    @Test
    public void testFloat() {
        assertEquals(5, NumberUtils.getScale(12.45678f));
        assertEquals(5, NumberUtils.getScale(Float.valueOf(12.45678f)));
        assertEquals(15, NumberUtils.getScale(Float.valueOf(12.45678f).doubleValue()));
        assertEquals(0, NumberUtils.getScale(Float.NaN));
        assertEquals(0, NumberUtils.getScale(Float.NEGATIVE_INFINITY));
        assertEquals(0, NumberUtils.getScale(Float.POSITIVE_INFINITY));
    }

    @Test
    public void testBigDecimal() {
        assertEquals(36, NumberUtils.getScale(new BigDecimal("12.123456789123456789123456789123456789")));
    }

    @Test
    public void testRoundValue() {
        assertEquals(0.35, NumberUtils.roundValue(0.35, 2), 0.001);

        assertEquals(0.3, NumberUtils.roundValue(0.34, 1), 0.01);
        assertEquals(0.4, NumberUtils.roundValue(0.35, 1), 0.01);
        assertEquals(0.4, NumberUtils.roundValue(0.36, 1), 0.01);

        assertNull(NumberUtils.roundValue(null, 1));
    }

    @Test
    public void testInfinity() {
        assertEquals(Double.POSITIVE_INFINITY, NumberUtils.roundValue(Double.POSITIVE_INFINITY, 1), 0.01);
        assertEquals(Double.NEGATIVE_INFINITY, NumberUtils.roundValue(Double.NEGATIVE_INFINITY, 1), 0.01);
        assertEquals(Double.NaN, NumberUtils.roundValue(Double.NaN, 1), 0.01);
    }

    @Test
    public void testConvertToDouble() {
        assertEquals("20.9", NumberUtils.convertToDouble(20.9d).toString());
        assertEquals("20.9", NumberUtils.convertToDouble(20.9f).toString());
        assertEquals("20.9", NumberUtils.convertToDouble(BigDecimal.valueOf(20.9)).toString());
    }

}
