package org.openl.meta;

import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TestDoubleValue {
    @Test
    public void testEquals() {
        DoubleValue value1 = new DoubleValue(10.2);
        DoubleValue value2 = new DoubleValue(10.2);
        assertEquals(value1, value2);
        value2.setMetaInfo(value1.getMetaInfo());
        assertEquals(value1, value2);
    }

    @Test
    public void testAdd() {
        DoubleValue v1 = new DoubleValue(13.33);
        DoubleValue v2 = new DoubleValue(11.11);
        assertEquals(24.44, DoubleValue.add(v1, v2).doubleValue(), 0.001);

        v2 = new DoubleValue(0);
        assertEquals(13.33, DoubleValue.add(v1, v2).doubleValue(), 0.001);

        v1 = new DoubleValue(0);
        v2 = new DoubleValue(0);
        assertEquals(0, DoubleValue.add(v1, v2).doubleValue(), 0.001);

        v1 = new DoubleValue(14.25);
        v2 = null;
        assertEquals(14.25, DoubleValue.add(v1, v2).doubleValue(), 0.001);

        v1 = null;
        v2 = new DoubleValue(17.88);
        assertEquals(17.88, DoubleValue.add(v1, v2).doubleValue(), 0.001);

        v1 = null;
        v2 = null;
        assertNull(DoubleValue.add(v1, v2));

        assertEquals("0.0", DoubleValue.add((DoubleValue) null, DoubleValue.ZERO).toString());
        assertEquals("0.0", DoubleValue.add(DoubleValue.ZERO, (DoubleValue) null).toString());
    }

    @Test
    public void testSubtract() {
        DoubleValue v1 = new DoubleValue(45.55);
        DoubleValue v2 = new DoubleValue(22.22);
        assertEquals(23.33, DoubleValue.subtract(v1, v2).doubleValue(), 0.001);

        v1 = new DoubleValue(0);
        assertEquals(-22.22, DoubleValue.subtract(v1, v2).doubleValue(), 0.001);

        v1 = new DoubleValue(45.55);
        v2 = new DoubleValue(0);
        assertEquals(45.55, DoubleValue.subtract(v1, v2).doubleValue(), 0.001);

        v1 = null;
        v2 = new DoubleValue(22.22);
        assertEquals(-22.22, DoubleValue.subtract(v1, v2).doubleValue(), 0.001);

        v1 = new DoubleValue(22.22);
        v2 = null;
        assertEquals(22.22, DoubleValue.subtract(v1, v2).doubleValue(), 0.001);

        v1 = null;
        v2 = new DoubleValue(225.22);
        assertEquals(-225.22, DoubleValue.subtract(v1, v2).doubleValue(), 0.001);

        v1 = null;
        v2 = null;
        assertNull(DoubleValue.subtract(v1, v2));
    }

    @Test
    public void testMultiply() {
        DoubleValue v1 = new DoubleValue(2);
        DoubleValue v2 = new DoubleValue(22.22);
        assertEquals(44.44, DoubleValue.multiply(v1, v2).doubleValue(), 0.001);

        v1 = new DoubleValue(0);
        v2 = new DoubleValue(22.22);
        assertEquals(0, DoubleValue.multiply(v1, v2).doubleValue(), 0.001);

        v1 = new DoubleValue(0);
        v2 = new DoubleValue(0);
        assertEquals(0, DoubleValue.multiply(v1, v2).doubleValue(), 0.001);

        v1 = new DoubleValue(2);
        v2 = new DoubleValue(-5.55);
        assertEquals(-11.1, DoubleValue.multiply(v1, v2).doubleValue(), 0.001);

        v1 = new DoubleValue(-1);
        v2 = new DoubleValue(-22.22);
        assertEquals(22.22, DoubleValue.multiply(v1, v2).doubleValue(), 0.001);

        v1 = new DoubleValue(0);
        v2 = new DoubleValue(-22.22);
        assertEquals(0, DoubleValue.multiply(v1, v2).doubleValue(), 0.001);

        v1 = null;
        v2 = new DoubleValue(22.22);
        assertEquals(22.22, DoubleValue.multiply(v1, v2).doubleValue(), 0.001);

        v1 = new DoubleValue(25.29);
        v2 = null;
        assertEquals(25.29, DoubleValue.multiply(v1, v2).doubleValue(), 0.001);

        v1 = null;
        v2 = null;
        assertNull(DoubleValue.multiply(v1, v2));

        v1 = new DoubleValue(11.22);
        v2 = new DoubleValue(0);
        assertEquals(0, DoubleValue.multiply(v1, v2).doubleValue(), 0.001);
    }

    @Test
    public void testDivide() {
        DoubleValue v1 = new DoubleValue(22.22);
        DoubleValue v2 = new DoubleValue(2);
        assertEquals(11.11, DoubleValue.divide(v1, v2).doubleValue(), 0.001);

        v1 = new DoubleValue(0);
        v2 = new DoubleValue(22.22);
        assertEquals(0, DoubleValue.divide(v1, v2).doubleValue(), 0.001);

        v1 = new DoubleValue(34);
        v2 = new DoubleValue(0.00000000);
        assertEquals(Double.POSITIVE_INFINITY, DoubleValue.divide(v1, v2).doubleValue(), 0.01);

        v1 = new DoubleValue(0);
        v2 = new DoubleValue(0.00000000);
        assertEquals(Double.POSITIVE_INFINITY, DoubleValue.divide(v1, v2).doubleValue(), 0.01);

        v1 = null;
        v2 = new DoubleValue(12);
        assertEquals(0.08333, DoubleValue.divide(v1, v2).doubleValue(), 0.001);

        v1 = new DoubleValue(34);
        v2 = null;

        assertEquals(34, DoubleValue.divide(v1, v2).doubleValue(), 0.1);

        v1 = null;
        v2 = null;
        assertNull(DoubleValue.divide(v1, v2));

        v1 = null;
        v2 = new DoubleValue(0);
        assertEquals(Double.POSITIVE_INFINITY, DoubleValue.divide(v1, v2).doubleValue(), 0.01);
    }

    @Test
    public void testRound() {
        DoubleValue value1 = new DoubleValue(1.23456789);

        assertEquals("1.2346", DoubleValue.round(value1, 4).toString());

        assertEquals("1.0", DoubleValue.round(value1, 0).toString());

        value1 = new DoubleValue(12.513456789);

        assertEquals("13.0", DoubleValue.round(value1, 0).toString());

        /**/
        DoubleValue v1 = new DoubleValue(0.7);
        DoubleValue v2 = new DoubleValue(0.75);

        DoubleValue res = DoubleValue.multiply(v1, v2);

        assertEquals("0.53", DoubleValue.round(res, 2).toString());

        value1 = new DoubleValue(12.6666667);
        assertEquals("12.67", DoubleValue.round(value1, 2).toString());

        value1 = new DoubleValue(12.9999999);
        assertEquals("13.0", DoubleValue.round(value1, 2).toString());

        value1 = new DoubleValue(12.6466667);
        assertEquals("12.65", DoubleValue.round(value1, 2).toString());

        assertNull(DoubleValue.round(null));
        assertNull(DoubleValue.round(null, 2));
        assertNull(DoubleValue.round(null, 2, BigDecimal.ROUND_HALF_UP));
    }

    @Test
    public void testRoundDouble() {
        DoubleValue value1 = new DoubleValue(1.23456789);

        assertEquals("1.0", DoubleValue.round(value1, wrap(0)).toString());

        assertEquals("1.2", DoubleValue.round(value1, wrap(0.1)).toString());
        assertEquals("1.23", DoubleValue.round(value1, wrap(0.01)).toString());
        assertEquals("1.235", DoubleValue.round(value1, wrap(0.001)).toString());
        assertEquals("1.2346", DoubleValue.round(value1, wrap(0.0001)).toString());
        assertEquals("1.23457", DoubleValue.round(value1, wrap(0.00001)).toString());
        assertEquals("1.234568", DoubleValue.round(value1, wrap(0.000001)).toString());
        assertEquals("1.2345679", DoubleValue.round(value1, wrap(0.0000001)).toString());
        assertEquals("1.23456789", DoubleValue.round(value1, wrap(0.00000001)).toString());
        assertEquals("1.23456789", DoubleValue.round(value1, wrap(0.000000001)).toString());

        assertEquals(1.2346, DoubleValue.round(value1, wrap(0.0002)).doubleValue(), 0.0002);
        assertEquals(1.2346, DoubleValue.round(value1, wrap(0.0003)).doubleValue(), 0.0003);
        // assertEquals(1.2346 - DoubleValue.round(value1, wrap(0.0004)).doubleValue(), 0, 0.0004 + Math.ulp(0.0004));
        assertEquals(1.2346, DoubleValue.round(value1, wrap(0.0005)).doubleValue(), 0.0005);

        value1 = new DoubleValue(12.513456789);

        assertEquals("13.0", DoubleValue.round(value1, wrap(0)).toString());

        value1 = new DoubleValue(130.07295);

        assertEquals("130.073", DoubleValue.round(value1, 4).toString());
        assertEquals("100.0", DoubleValue.round(value1, -2).toString());
        assertEquals("130.073", DoubleValue.round(value1, wrap(0.0001)).toString());

        value1 = new DoubleValue(111.41885);
        assertEquals("111.4189", DoubleValue.round(value1, 4).toString());
        System.out.println(value1.doubleValue());

        value1 = new DoubleValue(326.47365);

        assertEquals("326.4737", DoubleValue.round(value1, 4).toString());
        System.out.println(value1.doubleValue());

        // value1 = new DoubleValue(0.7 * 0.75);
        // assertEquals("0.53", DoubleValue.round(value1, 2).toString());

    }

    private DoubleValue wrap(double d) {
        return new DoubleValue(d);
    }
}
