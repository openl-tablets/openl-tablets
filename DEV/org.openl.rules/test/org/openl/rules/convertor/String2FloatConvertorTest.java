package org.openl.rules.convertor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

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

    @Test
    public void testConvertPositiveOverflow() {
        assertThrows(NumberFormatException.class, () -> {
            String2FloatConvertor converter = new String2FloatConvertor();
            converter.parse("1E39", null);
        });
    }

    @Test
    public void testConvertNegativeOverflow() {
        assertThrows(NumberFormatException.class, () -> {
            String2FloatConvertor converter = new String2FloatConvertor();
            converter.parse("-1E39", null);
        });
    }

}
