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
        String2NumberConverter<Number> conv = getNumberConverter();
        Number res = conv.parse("3.1415", null, null);
        assertEquals(3.1415d, res);
    }

    @Test
    public void testParseNegative() {
        String2NumberConverter<Number> conv = getNumberConverter();
        Number res = conv.parse("-2.1415", null, null);
        assertEquals(-2.1415d, res);
    }

    @Test
    public void testParseWithoutLeadingZero() {
        String2NumberConverter<Number> conv = getNumberConverter();
        Number res = conv.parse("-.123456789", null, null);
        assertEquals(-.123456789d, res);
    }

    @Test
    public void testParseWithLeadingZeros() {
        String2NumberConverter<Number> conv = getNumberConverter();
        Number res = conv.parse("0001.111000", null, null);
        assertEquals(1.111d, res);
    }

    @Test
    public void testParsePrecision() {
        String2NumberConverter<Number> conv = getNumberConverter();
        Number res = conv.parse("-1.99999999999999934", null, null);
        assertEquals(-1.99999999999999934d, res);
    }

    @Test
    public void testParseMaxLong() {
        String2NumberConverter<Number> conv = getNumberConverter();
        Number res = conv.parse("9223372036854775807", null, null);
        assertEquals(Long.MAX_VALUE, res);
    }

    @Test
    public void testParseMinLong() {
        String2NumberConverter<Number> conv = getNumberConverter();
        Number res = conv.parse("-9223372036854775808", null, null);
        assertEquals(Long.MIN_VALUE, res);
    }

    @Test
    public void testParseExcessMaxLong() {
        String2NumberConverter<Number> conv = getNumberConverter();
        Number res = conv.parse("9223372036854775808", null, null);
        assertEquals(9223372036854775808d, res);
    }

    @Test
    public void testParseExcessMinLong() {
        String2NumberConverter<Number> conv = getNumberConverter();
        Number res = conv.parse("-9223372036854775809", null, null);
        assertEquals(-9223372036854775809d, res);
    }


    @Test
    public void testParsePercents() {
        String2NumberConverter<Number> conv = getNumberConverter();
        Number res = conv.parse("17.5%", null, null);
        assertEquals(0.175d, res);
    }

    @Test
    public void testParseE() {
        String2NumberConverter<Number> conv = getNumberConverter();
        Number res = conv.parse("1.234E2", null, null);
        assertEquals(123.4d, res);
    }

    @Test
    public void testParseENegative() {
        String2NumberConverter<Number> conv = getNumberConverter();
        Number res = conv.parse("-1.23E-3", null, null);
        assertEquals(-0.00123d, res);
    }

    @Test
    public void testParseELong() {
        String2NumberConverter<Number> conv = getNumberConverter();
        Number res = conv.parse("-1.23E4", null, null);
        assertEquals(-12300L, res);
    }

    @Test
    public void testParseWithFormat() {
        String2NumberConverter<Number> conv = getNumberConverter();
        Number res = conv.parse("-3.1415$", "#,###$", null);
        assertEquals(-3.1415d, res);
    }

    @Test
    public void testFormat() {
        String2NumberConverter<Number> conv = getNumberConverter();
        String res = conv.format(3.1415d, null);
        assertEquals("3.1415", res);
    }

    @Test
    public void testFormatByPatternWithRound() {
        String2NumberConverter<Number> conv = getNumberConverter();
        String res = conv.format(3.1415d, "#,###.###");
        assertEquals("3.142", res);
    }

    @Test
    public void testFormatByPatternWithRequiredDigits() {
        String2NumberConverter<Number> conv = getNumberConverter();
        String res = conv.format(3.1d, "0,000.00");
        assertEquals("0,003.10", res);
    }

    @Test
    public void testFormatByPatternWithSymbols() {
        String2NumberConverter<Number> conv = getNumberConverter();
        String res = conv.format(1234.10, "$ #,000.0# USD");
        assertEquals("$ 1,234.1 USD", res);
    }

    @Test
    public void testParseNull() {
        String2NumberConverter<Number> conv = getNumberConverter();
        assertNull(conv.parse(null, null, null));
    }

    @Test
    public void testFormatNull() {
        String2NumberConverter<Number> conv = getNumberConverter();
        assertNull(conv.format(null, null));
    }

    @Test
    public void testParseNaN() {
        String2NumberConverter<Number> conv = getNumberConverter();
        Number res = conv.parse("NaN", null, null);
        assertEquals(Double.NaN, res);
    }

    @Test
    public void testFormatNaN() {
        String2NumberConverter<Number> conv = getNumberConverter();
        String res = conv.format(Double.NaN, null);
        assertEquals("NaN", res);
    }

    @Test
    public void testParsePlusInfinity() {
        String2NumberConverter<Number> conv = getNumberConverter();
        Number res = conv.parse("Infinity", null, null);
        assertEquals(Double.POSITIVE_INFINITY, res);
    }

    @Test
    public void testFormatPlusInfinity() {
        String2NumberConverter<Number> conv = getNumberConverter();
        String res = conv.format(Double.POSITIVE_INFINITY, null);
        assertEquals("Infinity", res);
    }

    @Test
    public void testParseMinusInfinity() {
        String2NumberConverter<Number> conv = getNumberConverter();
        Number res = conv.parse("-Infinity", null, null);
        assertEquals(Double.NEGATIVE_INFINITY, res);
    }

    @Test
    public void testFormatMinusInfinity() {
        String2NumberConverter<Number> conv = getNumberConverter();
        String res = conv.format(Double.NEGATIVE_INFINITY, "#.###");
        assertEquals("-Infinity", res);
    }

    @Test(expected = NumberFormatException.class)
    public void testParseNotNumber() {
        String2NumberConverter<Number> conv = getNumberConverter();
        conv.parse("3.1415d", null, null);
    }

    @Test(expected = NumberFormatException.class)
    public void testParseEmpty() {
        String2NumberConverter<Number> conv = getNumberConverter();
        conv.parse("", null, null);
    }

    @Test(expected = NumberFormatException.class)
    public void testParseNotENumber() {
        String2NumberConverter<Number> conv = getNumberConverter();
        conv.parse("1e1", null, null);
    }

    @Test(expected = NumberFormatException.class)
    public void testParseWithSpaces() {
        String2NumberConverter<Number> conv = getNumberConverter();
        conv.parse("1 ", null, null);
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
