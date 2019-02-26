package org.openl.rules.helpers;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.math.BigDecimal;

import org.junit.Test;
import org.openl.meta.BigDecimalValue;
import org.openl.meta.DoubleValue;

public class NumberUtilsTest {

    @Test
    public void testGetDoubleScale() {
        assertEquals(3, NumberUtils.getScale(12.678));
        assertEquals(1, NumberUtils.getScale(12.1));
        assertEquals(5, NumberUtils.getScale(12.67867));
        assertEquals(1, NumberUtils.getScale(0.2));

        try {
            NumberUtils.getScale(null);
            fail();
        } catch (NullPointerException e) {
            assertEquals("Null value is not supported", e.getMessage());
        }

        assertEquals(0, NumberUtils.getScale(Double.NaN));
        assertEquals(0, NumberUtils.getScale(Double.NEGATIVE_INFINITY));
    }

    @Test
    public void testGetNumberScale() {
        assertEquals(3, NumberUtils.getScale((Number) new Double(12.678)));
        assertEquals(0, NumberUtils.getScale(new Integer(12)));
        assertEquals(0, NumberUtils.getScale(new Integer(0)));

        try {
            NumberUtils.getScale((Number) null);
            fail();
        } catch (NullPointerException e) {
            assertEquals("Null value is not supported", e.getMessage());
        }

        assertEquals(0, NumberUtils.getScale((Number) Double.NaN));
        assertEquals(0, NumberUtils.getScale((Number) Double.NEGATIVE_INFINITY));
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
    public void testDoubleValue() {
        assertEquals(4, NumberUtils.getScale(new DoubleValue(1234.5553)));
        assertEquals(0, NumberUtils.getScale(new DoubleValue(Double.NaN)));
        assertEquals(0, NumberUtils.getScale(new DoubleValue(Double.NEGATIVE_INFINITY)));
        assertEquals(0, NumberUtils.getScale(new DoubleValue(Double.POSITIVE_INFINITY)));
    }

    @Test
    public void testBigDecimal() {
        assertEquals(36, NumberUtils.getScale(new BigDecimal("12.123456789123456789123456789123456789")));
        assertEquals(35,
                NumberUtils.getScale(new BigDecimalValue(new BigDecimal("12.12345678912345678912345678912345678"))));
    }

    @Test
    public void testRoundValue() {
        assertEquals(0.35, NumberUtils.roundValue(0.35, 2).doubleValue(), 0.001);

        assertEquals(0.3, NumberUtils.roundValue(0.34, 1).doubleValue(), 0.01);
        assertEquals(0.4, NumberUtils.roundValue(0.35, 1).doubleValue(), 0.01);
        assertEquals(0.4, NumberUtils.roundValue(0.36, 1).doubleValue(), 0.01);

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
