package org.openl.rules.convertor;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class String2IntConvertorTest {

    @Test
    public void testConvertPositive() {
        String2IntConvertor converter = new String2IntConvertor();
        Number result = converter.parse("2147483647", null, null);
        assertEquals(Integer.MAX_VALUE, result);
    }

    @Test
    public void testConvertNegative() {
        String2IntConvertor converter = new String2IntConvertor();
        Number result = converter.parse("-2147483648", null, null);
        assertEquals(Integer.MIN_VALUE, result);
    }

    @Test(expected = NumberFormatException.class)
    public void testConvertPositiveOverflow() {
        String2IntConvertor converter = new String2IntConvertor();
        converter.parse("2147483648", null, null);
    }

    @Test(expected = NumberFormatException.class)
    public void testConvertNegativeOverflow() {
        String2IntConvertor converter = new String2IntConvertor();
        converter.parse("-2147483649", null, null);
    }

    @Test(expected = NumberFormatException.class)
    public void testConvertNonInteger() {
        String2IntConvertor converter = new String2IntConvertor();
        converter.parse("1.3", null, null);
    }

    @Test
    public void testFormat() {
        String2IntConvertor converter = new String2IntConvertor();
        String result = converter.format(Integer.MIN_VALUE, null);
        assertEquals("-2147483648", result);
    }
}
