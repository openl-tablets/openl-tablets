package org.openl.rules.convertor;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class String2FloatConvertorTest {

    @Test
    public void testConvertPositive() {
        String2FloatConvertor converter = new String2FloatConvertor();
        Number result = converter.parse("123.456", null);
        assertEquals(123.456f, result);
    }

    @Test
    public void testConvertNegative() {
        String2FloatConvertor converter = new String2FloatConvertor();
        Number result = converter.parse("-123.456", null);
        assertEquals(-123.456f, result);
    }

    @Test
    public void testConvertPositiveInfinity() {
        String2FloatConvertor converter = new String2FloatConvertor();
        Number result = converter.parse("Infinity", null);
        assertEquals(Float.POSITIVE_INFINITY, result);
    }

    @Test
    public void testConvertNegativeInfinity() {
        String2FloatConvertor converter = new String2FloatConvertor();
        Number result = converter.parse("-Infinity", null);
        assertEquals(Float.NEGATIVE_INFINITY, result);
    }

    @Test(expected = NumberFormatException.class)
    public void testConvertPositiveOverflow() {
        String2FloatConvertor converter = new String2FloatConvertor();
        converter.parse("1E39", null);
    }

    @Test(expected = NumberFormatException.class)
    public void testConvertNegativeOverflow() {
        String2FloatConvertor converter = new String2FloatConvertor();
        converter.parse("-1E39", null);
    }

}
