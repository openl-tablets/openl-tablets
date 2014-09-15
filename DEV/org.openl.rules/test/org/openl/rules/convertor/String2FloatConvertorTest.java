package org.openl.rules.convertor;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

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

    @Test
    public void testFormat() {
        String2FloatConvertor converter = new String2FloatConvertor();
        String result = converter.format(98765.43f, null);
        assertEquals("98765.43", result);
    }

    @Test
    public void testFormatNegative() {
        String2FloatConvertor converter = new String2FloatConvertor();
        String result = converter.format(-98765.43f, null);
        assertEquals("-98765.43", result);
    }

    @Test
    public void testFormatZero() {
        String2FloatConvertor converter = new String2FloatConvertor();
        String result = converter.format(0f, null);
        assertEquals("0.0", result);
    }

    @Test
    public void testFormatPrecision() {
        String2FloatConvertor converter = new String2FloatConvertor();
        String result = converter.format(0.000000000012345678f, null);
        assertEquals("0.000000000012345678", result);
    }

    @Test
    public void testFormatNegativePrecision() {
        String2FloatConvertor converter = new String2FloatConvertor();
        String result = converter.format(-0.000000000012345678f, null);
        assertEquals("-0.000000000012345678", result);
    }

    @Test
    public void testFormatMin() {
        String2FloatConvertor converter = new String2FloatConvertor();
        String result = converter.format(Float.MIN_VALUE, null);
        assertEquals("0.0000000000000000000000000000000000000000000014", result);
    }

    @Test
    public void testFormatNegativeMin() {
        String2FloatConvertor converter = new String2FloatConvertor();
        String result = converter.format(-Float.MIN_VALUE, null);
        assertEquals("-0.0000000000000000000000000000000000000000000014", result);
    }

    @Test
    public void testFormatMax() {
        String2FloatConvertor converter = new String2FloatConvertor();
        String result = converter.format(Float.MAX_VALUE, null);
        assertEquals("340282350000000000000000000000000000000.0", result);
    }

    @Test
    public void testFormatNegativeMax() {
        String2FloatConvertor converter = new String2FloatConvertor();
        String result = converter.format(-Float.MAX_VALUE, null);
        assertEquals("-340282350000000000000000000000000000000.0", result);
    }

    @Test
    public void testFormatInfinity() {
        String2FloatConvertor converter = new String2FloatConvertor();
        String result = converter.format(Float.POSITIVE_INFINITY, null);
        assertEquals("Infinity", result);
    }

    @Test
    public void testFormatNegativeInfinity() {
        String2FloatConvertor converter = new String2FloatConvertor();
        String result = converter.format(Float.NEGATIVE_INFINITY, null);
        assertEquals("-Infinity", result);
    }
}
