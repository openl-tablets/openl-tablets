package org.openl.rules.convertor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class String2FloatConvertorTest {

    @Test
    void testConvertPositive() {
        var converter = new String2FloatConvertor();
        var result = converter.parse("123.456", null);
        assertEquals(123.456f, result);
    }

    @Test
    void testConvertNegative() {
        var converter = new String2FloatConvertor();
        var result = converter.parse("-123.456", null);
        assertEquals(-123.456f, result);
    }

    @Test
    void testConvertPositiveInfinity() {
        var converter = new String2FloatConvertor();
        var result = converter.parse("Infinity", null);
        assertEquals(Float.POSITIVE_INFINITY, result);
    }

    @Test
    void testConvertNegativeInfinity() {
        var converter = new String2FloatConvertor();
        var result = converter.parse("-Infinity", null);
        assertEquals(Float.NEGATIVE_INFINITY, result);
    }

    @Test
    void testConvertPositiveOverflow() {
        assertThrows(NumberFormatException.class, () -> {
            var converter = new String2FloatConvertor();
            converter.parse("1E39", null);
        });
    }

    @Test
    void testConvertNegativeOverflow() {
        assertThrows(NumberFormatException.class, () -> {
            var converter = new String2FloatConvertor();
            converter.parse("-1E39", null);
        });
    }

}
