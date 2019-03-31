package org.openl.meta;

import static org.junit.Assert.*;

import java.math.BigDecimal;

import org.junit.Test;

public class TestFloatValue {
    @Test
    public void testRound() {
        FloatValue value1 = new FloatValue(1.23456789f);

        assertEquals("1.235", FloatValue.round(value1, 3).toString());

        assertEquals("1.0", FloatValue.round(value1, 0).toString());

        value1 = new FloatValue(12.513456f);

        assertEquals("13.0", FloatValue.round(value1, 0).toString());

        value1 = new FloatValue(7.525f);

        assertEquals("7.53", FloatValue.round(value1, 2).toString());

        FloatValue v1 = new FloatValue(0.7f);
        FloatValue v2 = new FloatValue(0.75f);

        FloatValue res = FloatValue.multiply(v1, v2);

        assertNotSame("0.53", FloatValue.round(res, 2).toString());

        value1 = new FloatValue(12.6666667f);
        assertEquals("12.67", FloatValue.round(value1, 2).toString());

        value1 = new FloatValue(12.9999999f);
        assertEquals("13.0", FloatValue.round(value1, 2).toString());

        value1 = new FloatValue(12.6466667f);
        assertEquals("12.65", FloatValue.round(value1, 2).toString());

        assertNull(FloatValue.round(null));
        assertNull(FloatValue.round(null, 2));
        assertNull(FloatValue.round(null, 2, BigDecimal.ROUND_HALF_UP));
    }

    @Test
    public void testAdd() {
        assertEquals("0.0", FloatValue.add((FloatValue) null, new FloatValue(0)).toString());
        assertEquals("0.0", FloatValue.add(new FloatValue(0), (FloatValue) null).toString());
    }
}
