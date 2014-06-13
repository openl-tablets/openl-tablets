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
        String2NumberConverter<Double> conv = getNumberConverter();
        Object res = conv.parse("3.1415", null, null);
        assertEquals(3.1415d, res);
    }

    @Test(expected = NumberFormatException.class)
    public void testParseNonNumber() {
        String2NumberConverter<Double> conv = getNumberConverter();
        conv.parse("3.1415d", null, null);
    }

    @Test(expected = NumberFormatException.class)
    public void testParseEmpty() {
        String2NumberConverter<Double> conv = getNumberConverter();
        conv.parse("", null, null);
    }

    @Test
    public void testParsePercents() {
        String2NumberConverter<Double> conv = getNumberConverter();
        Object res = conv.parse("17.5%", null, null);
        assertEquals(0.175d, res);
    }

    @Test
    public void testFormat() {
        String2NumberConverter<Double> conv = getNumberConverter();
        String res = conv.format(3.1415d, null);
        assertEquals("3.1415", res);
    }

    @Test
    public void testFormatByPattern() {
        String2NumberConverter<Double> conv = getNumberConverter();
        String res = conv.format(3.1415d, "#.###");
        assertEquals("3.142", res);
    }

    @Test
    public void testParseNull() {
        String2NumberConverter<Double> conv = getNumberConverter();
        assertNull(conv.parse(null, null, null));
    }

    @Test
    public void testFormatNull() {
        String2NumberConverter<Double> conv = getNumberConverter();
        assertNull(conv.format(null, null));
    }

    @Test
    public void testParseNaN() {
        String2NumberConverter<Double> conv = getNumberConverter();
        Object res = conv.parse("NaN", null, null);
        assertEquals(Double.NaN, res);
    }

    @Test
    public void testFormatNaN() {
        String2NumberConverter<Double> conv = getNumberConverter();
        String res = conv.format(Double.NaN, null);
        assertEquals("NaN", res);
    }

    @Test
    public void testParsePlusInfinity() {
        String2NumberConverter<Double> conv = getNumberConverter();
        Object res = conv.parse("Infinity", null, null);
        assertEquals(Double.POSITIVE_INFINITY, res);
    }

    @Test
    public void testFormatPlusInfinity() {
        String2NumberConverter<Double> conv = getNumberConverter();
        String res = conv.format(Double.POSITIVE_INFINITY, null);
        assertEquals("Infinity", res);
    }

    @Test
    public void testParseMinusInfinity() {
        String2NumberConverter<Double> conv = getNumberConverter();
        Object res = conv.parse("-Infinity", null, null);
        assertEquals(Double.NEGATIVE_INFINITY, res);
    }

    @Test
    public void testFormatMinusInfinity() {
        String2NumberConverter<Double> conv = getNumberConverter();
        String res = conv.format(Double.NEGATIVE_INFINITY, "#.###");
        assertEquals("-Infinity", res);
    }


    private String2NumberConverter<Double> getNumberConverter() {
        return new String2NumberConverter<Double>() {
            @Override
            Double convert(Number number, String data) {
                return number.doubleValue();
            }
        };
    }
}
