package org.openl.rules.convertor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class String2NumberConverterTest {

    private Locale defaultLocale;

    @Before
    public void setupLocale() {
        defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.GERMAN);
    }

    @After
    public void restoreLocale() {
        Locale.setDefault(defaultLocale);
    }

    @Test
    public void testParse() {
        String2NumberConverter<Number> converter = getNumberConverter();
        Number result = converter.parse("3.1415", null);
        assertEquals(3.1415d, result);
    }

    @Test
    public void testParseNegative() {
        String2NumberConverter<Number> converter = getNumberConverter();
        Number result = converter.parse("-2.1415", null);
        assertEquals(-2.1415d, result);
    }

    @Test
    public void testParseWithoutLeadingZero() {
        String2NumberConverter<Number> converter = getNumberConverter();
        Number result = converter.parse("-.123456789", null);
        assertEquals(-.123456789d, result);
    }

    @Test
    public void testParseWithLeadingZeros() {
        String2NumberConverter<Number> converter = getNumberConverter();
        Number result = converter.parse("0001.111000", null);
        assertEquals(1.111d, result);
    }

    @Test
    public void testParsePrecision() {
        String2NumberConverter<Number> converter = getNumberConverter();
        Number result = converter.parse("-1.99999999999999934", null);
        assertEquals(-1.99999999999999934d, result);
    }

    @Test
    public void testParseMaxLong() {
        String2NumberConverter<Number> converter = getNumberConverter();
        Number result = converter.parse("9223372036854775807", null);
        assertEquals(Long.MAX_VALUE, result);
    }

    @Test
    public void testParseMinLong() {
        String2NumberConverter<Number> converter = getNumberConverter();
        Number result = converter.parse("-9223372036854775808", null);
        assertEquals(Long.MIN_VALUE, result);
    }

    @Test
    public void testParseExcessMaxLong() {
        String2NumberConverter<Number> converter = getNumberConverter();
        Number result = converter.parse("9223372036854775808", null);
        assertEquals(9223372036854775808d, result);
    }

    @Test
    public void testParseExcessMinLong() {
        String2NumberConverter<Number> converter = getNumberConverter();
        Number result = converter.parse("-9223372036854775809", null);
        assertEquals(-9223372036854775809d, result);
    }


    @Test
    public void testParsePercents() {
        String2NumberConverter<Number> converter = getNumberConverter();
        Number result = converter.parse("17.5%", null);
        assertEquals(0.175d, result);
    }

    @Test
    public void testParseE() {
        String2NumberConverter<Number> converter = getNumberConverter();
        Number result = converter.parse("1.234E2", null);
        assertEquals(123.4d, result);
    }

    @Test
    public void testParseENegative() {
        String2NumberConverter<Number> converter = getNumberConverter();
        Number result = converter.parse("-1.23E-3", null);
        assertEquals(-0.00123d, result);
    }

    @Test
    public void testParseELong() {
        String2NumberConverter<Number> converter = getNumberConverter();
        Number result = converter.parse("-1.23E4", null);
        assertEquals(-12300L, result);
    }

    @Test
    public void testParseWithFormat() {
        String2NumberConverter<Number> converter = getNumberConverter();
        Number result = converter.parse("-3.1415$", "#,###$");
        assertEquals(-3.1415d, result);
    }

    @Test
    public void testFormat() {
        String2NumberConverter<Number> converter = getNumberConverter();
        String result = converter.format(1234567.8901d, null);
        assertEquals("1234567.8901", result);
    }

    @Test
    public void testFormat2() {
        String2NumberConverter<Number> converter = getNumberConverter();
        String result = converter.format(1234567.8901d, "#,###.###");
        assertEquals("1,234,567.89", result);
    }

    @Test
    public void testFormatZero() {
        String2NumberConverter<Number> converter = getNumberConverter();
        String result = converter.format(0d, null);
        assertEquals("0", result);
    }

    @Test
    public void testFormatFraction() {
        String2NumberConverter<Number> converter = getNumberConverter();
        String result = converter.format(0.001d, null);
        assertEquals("0.001", result);
    }

    @Test
    public void testFormatZeroEmptyFormat() {
        String2NumberConverter<Number> converter = getNumberConverter();
        String result = converter.format(0d, "");
        assertEquals("0", result);
    }

    @Test
    public void testFormatFractionEmptyFormat() {
        String2NumberConverter<Number> converter = getNumberConverter();
        String result = converter.format(0.001d, "");
        assertEquals(".001", result);
    }

    @Test
    public void testFormatByPatternWithRound() {
        String2NumberConverter<Number> converter = getNumberConverter();
        String result = converter.format(3.1415d, "#,###.###");
        assertEquals("3.142", result);
    }

    @Test
    public void testFormatByPatternWithRequiredDigits() {
        String2NumberConverter<Number> converter = getNumberConverter();
        String result = converter.format(3.1d, "0,000.00");
        assertEquals("0,003.10", result);
    }

    @Test
    public void testFormatByPatternWithSymbols() {
        String2NumberConverter<Number> converter = getNumberConverter();
        String result = converter.format(1234.10, "$ #,000.0# USD");
        assertEquals("$ 1,234.1 USD", result);
    }

    @Test
    public void testParseNull() {
        String2NumberConverter<Number> converter = getNumberConverter();
        assertNull(converter.parse(null, null));
    }

    @Test
    public void testFormatNull() {
        String2NumberConverter<Number> converter = getNumberConverter();
        assertNull(converter.format(null, null));
    }

    @Test
    public void testParseNaN() {
        String2NumberConverter<Number> converter = getNumberConverter();
        Number result = converter.parse("NaN", null);
        assertEquals(Double.NaN, result);
    }

    @Test
    public void testFormatNaN() {
        String2NumberConverter<Number> converter = getNumberConverter();
        String result = converter.format(Double.NaN, null);
        assertEquals("NaN", result);
    }

    @Test
    public void testParsePlusInfinity() {
        String2NumberConverter<Number> converter = getNumberConverter();
        Number result = converter.parse("Infinity", null);
        assertEquals(Double.POSITIVE_INFINITY, result);
    }

    @Test
    public void testFormatPlusInfinity() {
        String2NumberConverter<Number> converter = getNumberConverter();
        String result = converter.format(Double.POSITIVE_INFINITY, null);
        assertEquals("Infinity", result);
    }

    @Test
    public void testParseMinusInfinity() {
        String2NumberConverter<Number> converter = getNumberConverter();
        Number result = converter.parse("-Infinity", null);
        assertEquals(Double.NEGATIVE_INFINITY, result);
    }

    @Test
    public void testFormatMinusInfinity() {
        String2NumberConverter<Number> converter = getNumberConverter();
        String result = converter.format(Double.NEGATIVE_INFINITY, "#.###");
        assertEquals("-Infinity", result);
    }

    @Test(expected = NumberFormatException.class)
    public void testParseNotNumber() {
        String2NumberConverter<Number> converter = getNumberConverter();
        converter.parse("3.1415d", null);
    }

    @Test(expected = NumberFormatException.class)
    public void testParseEmpty() {
        String2NumberConverter<Number> converter = getNumberConverter();
        // skip using a String Pool in runtime
        converter.parse(new String(""), null);
    }

    @Test(expected = NumberFormatException.class)
    public void testParsePercentSign() {
        String2NumberConverter<Number> converter = getNumberConverter();
        // skip using a String Pool in runtime
        converter.parse(new String("%"), null);
    }

    @Test(expected = NumberFormatException.class)
    public void testParseNotENumber() {
        String2NumberConverter<Number> converter = getNumberConverter();
        converter.parse("1e1", null);
    }

    @Test(expected = NumberFormatException.class)
    public void testParseWithSpaces() {
        String2NumberConverter<Number> converter = getNumberConverter();
        converter.parse("1 ", null);
    }

    private String2NumberConverter<Number> getNumberConverter() {
        return new String2NumberConverter<Number>() {
            @Override
            Number convert(Number number, String data) {
                return number;
            }
        };
    }
}
