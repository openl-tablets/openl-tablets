package org.openl.rules.convertor;

import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

public class String2DoubleConvertorTest {

    @Test
    public void testConvertPositive() {
        String2DoubleConvertor converter = new String2DoubleConvertor();
        Number result = converter.parse("123.125", null, null);
        assertEquals(123.125d, result);
    }

    @Test
    public void testConvertNegative() {
        String2DoubleConvertor converter = new String2DoubleConvertor();
        Number result = converter.parse("-123.125", null, null);
        assertEquals(-123.125d, result);
    }

    @Test
    public void testConvertPositiveOverflow() {
        String2DoubleConvertor converter = new String2DoubleConvertor();
        Number result = converter.parse("10E500", null, null);
        assertEquals(Double.POSITIVE_INFINITY, result);
    }

    @Test
    public void testConvertNegativeOverflow() {
        String2DoubleConvertor converter = new String2DoubleConvertor();
        Number result = converter.parse("-10E500", null, null);
        assertEquals(Double.NEGATIVE_INFINITY, result);
    }

    @Test
    public void testFormat() {
        String2DoubleConvertor converter = new String2DoubleConvertor();
        String result = converter.format(1234567890.12345, null);
        assertEquals("1234567890.12", result);
    }
}
