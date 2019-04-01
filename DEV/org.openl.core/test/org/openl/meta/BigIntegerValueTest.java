package org.openl.meta;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class BigIntegerValueTest {

    @Test
    public void testEquals() {
        BigIntegerValue value1 = new BigIntegerValue("1234");
        BigIntegerValue value2 = new BigIntegerValue("1234");
        assertEquals(value1, value2);
        value2.setMetaInfo(value1.getMetaInfo());
        assertEquals(value1, value2);
    }

    @Test
    public void testMax() {
        BigIntegerValue value1 = new BigIntegerValue("1234");
        BigIntegerValue value2 = new BigIntegerValue("12345");
        assertEquals(value2, BigIntegerValue.max(value1, value2));

        value1 = null;
        value2 = new BigIntegerValue("12345");
        assertEquals(value2, BigIntegerValue.max(value1, value2));

        value1 = new BigIntegerValue("12345");
        value2 = null;
        assertEquals(value1, BigIntegerValue.max(value1, value2));

        value1 = null;
        value2 = null;
        assertNull(BigIntegerValue.max(value1, value2));
    }

    @Test
    public void testMin() {
        BigIntegerValue value1 = new BigIntegerValue("12");
        BigIntegerValue value2 = new BigIntegerValue("12345");
        assertEquals(value1, BigIntegerValue.min(value1, value2));

        value1 = null;
        value2 = new BigIntegerValue("12345");

        assertEquals(value2, BigIntegerValue.min(value1, value2));

        value1 = new BigIntegerValue("12");
        value2 = null;
        assertEquals(value1, BigIntegerValue.min(value1, value2));

        value1 = null;
        value2 = null;
        assertNull(BigIntegerValue.min(value1, value2));

    }

    @Test
    public void testAutocastLong() {
        BigIntegerValue expectedResult = new BigIntegerValue("1234");

        BigIntegerValue result = BigIntegerValue.autocast((long) 1234, null);

        assertEquals(expectedResult, result);
    }

    @Test
    public void testAdd() {
        assertEquals("0", BigIntegerValue.add((BigIntegerValue) null, new BigIntegerValue("0")).toString());
        assertEquals("0", BigIntegerValue.add(new BigIntegerValue("0"), (BigIntegerValue) null).toString());
    }
}
