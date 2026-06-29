package org.openl.rules.convertor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class String2DoubleConvertorTest {

    @Test
    void testConvertPositive() {
        String2DoubleConvertor converter = new String2DoubleConvertor();
        Number result = converter.parse("123.125", null);
        assertEquals(123.125d, result);
    }

    @Test
    void testConvertNegative() {
        String2DoubleConvertor converter = new String2DoubleConvertor();
        Number result = converter.parse("-123.125", null);
        assertEquals(-123.125d, result);
    }

    @Test
    void testConvertPositiveOverflow() {
        String2DoubleConvertor converter = new String2DoubleConvertor();
        Number result = converter.parse("10E500", null);
        assertEquals(Double.POSITIVE_INFINITY, result);
    }

    @Test
    void testConvertNegativeOverflow() {
        String2DoubleConvertor converter = new String2DoubleConvertor();
        Number result = converter.parse("-10E500", null);
        assertEquals(Double.NEGATIVE_INFINITY, result);
    }

}
