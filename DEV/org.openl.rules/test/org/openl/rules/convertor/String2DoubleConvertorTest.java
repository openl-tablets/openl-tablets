package org.openl.rules.convertor;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class String2DoubleConvertorTest {

    @Test
    public void testConvertPositive() {
        String2DoubleConvertor converter = new String2DoubleConvertor();
        Number result = converter.parse("123.125", null);
        assertEquals(123.125d, result);
    }

    @Test
    public void testConvertNegative() {
        String2DoubleConvertor converter = new String2DoubleConvertor();
        Number result = converter.parse("-123.125", null);
        assertEquals(-123.125d, result);
    }

    @Test
    public void testConvertPositiveOverflow() {
        String2DoubleConvertor converter = new String2DoubleConvertor();
        Number result = converter.parse("10E500", null);
        assertEquals(Double.POSITIVE_INFINITY, result);
    }

    @Test
    public void testConvertNegativeOverflow() {
        String2DoubleConvertor converter = new String2DoubleConvertor();
        Number result = converter.parse("-10E500", null);
        assertEquals(Double.NEGATIVE_INFINITY, result);
    }

    @Test
    public void testFormat() {
        String2DoubleConvertor converter = new String2DoubleConvertor();
        String result = converter.format(1234567890.12345d, null);
        assertEquals("1234567890.12345", result);
    }

    @Test
    public void testFormatZero() {
        String2DoubleConvertor converter = new String2DoubleConvertor();
        String result = converter.format(0d, null);
        assertEquals("0.0", result);
    }

    @Test
    public void testFormatPrecision() {
        String2DoubleConvertor converter = new String2DoubleConvertor();
        String result = converter.format(0.00000000001234567890123456d, null);
        assertEquals("0.00000000001234567890123456", result);
    }
}
