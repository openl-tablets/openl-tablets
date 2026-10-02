package org.openl.rules.convertor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class String2DoubleConvertorTest {

    @Test
    void testConvertPositive() {
        var converter = new String2DoubleConvertor();
        var result = converter.parse("123.125", null);
        assertEquals(123.125d, result);
    }

    @Test
    void testConvertNegative() {
        var converter = new String2DoubleConvertor();
        var result = converter.parse("-123.125", null);
        assertEquals(-123.125d, result);
    }

    @Test
    void testConvertPositiveOverflow() {
        var converter = new String2DoubleConvertor();
        var result = converter.parse("10E500", null);
        assertEquals(Double.POSITIVE_INFINITY, result);
    }

    @Test
    void testConvertNegativeOverflow() {
        var converter = new String2DoubleConvertor();
        var result = converter.parse("-10E500", null);
        assertEquals(Double.NEGATIVE_INFINITY, result);
    }

    @Test
    void testConvertPercent() {
        var converter = new String2DoubleConvertor();
        assertEquals(0.175d, converter.parse("17.5%", null));
        assertEquals(-0.15d, converter.parse("-15%", null));
        assertEquals(3d, converter.parse("300%", null));

        // Dividing the parsed double by 100 missed these fractions in the last bit
        assertEquals(0.0007d, converter.parse("0.07%", null));
        assertEquals(0.011d, converter.parse("1.1%", null));
        assertEquals(0.333d, converter.parse("33.3%", null));
        assertEquals(0.9999d, converter.parse("99.99%", null));
        assertEquals(-0.0007d, converter.parse("-0.07%", null));
    }

    @Test
    void testConvertNegativeZero() {
        var converter = new String2DoubleConvertor();
        assertEquals(-0d, converter.parse("-0", null));
        assertEquals(-0d, converter.parse("-0.0", null));
        assertEquals(-0d, converter.parse("-0%", null));
        assertEquals(-0d, converter.parse("-1E-400", null));
        assertEquals(0d, converter.parse("0", null));
    }

    @Test
    void testConvertNegativeZeroOfFormat() {
        var converter = String2DataConvertorFactory.getConvertor(Double.class);
        assertEquals(-0d, converter.parse("(0)", "0;(0)"));
        assertEquals(-1d, converter.parse("(1)", "0;(0)"));
        assertEquals(0d, converter.parse("0", "0;(0)"));
    }

}
