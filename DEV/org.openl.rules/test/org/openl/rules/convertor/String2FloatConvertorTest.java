package org.openl.rules.convertor;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class String2FloatConvertorTest {

    @Test
    public void testConvertPositive() {
        String2FloatConvertor conv = new String2FloatConvertor();
        Number result = conv.convert(123.456d, "");
        assertEquals(123.456f, result);
    }

    @Test
    public void testConvertNegative() {
        String2FloatConvertor conv = new String2FloatConvertor();
        Number result = conv.convert(-123.456d, "");
        assertEquals(-123.456f, result);
    }

    @Test
    public void testConvertPositiveInfinity() {
        String2FloatConvertor conv = new String2FloatConvertor();
        Number result = conv.convert(Double.POSITIVE_INFINITY, "");
        assertEquals(Float.POSITIVE_INFINITY, result);
    }

    @Test
    public void testConvertNegativeInfinity() {
        String2FloatConvertor conv = new String2FloatConvertor();
        Number result = conv.convert(Double.NEGATIVE_INFINITY, "");
        assertEquals(Float.NEGATIVE_INFINITY, result);
    }

    @Test(expected = NumberFormatException.class)
    public void testConvertPositiveOverflow() {
        String2FloatConvertor conv = new String2FloatConvertor();
        conv.convert(1e39d, "");
    }

    @Test(expected = NumberFormatException.class)
    public void testConvertNegativeOverflow() {
        String2FloatConvertor conv = new String2FloatConvertor();
        conv.convert(-1e39d, "");
    }
}
