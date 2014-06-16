package org.openl.rules.convertor;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class String2ShortConvertorTest {

    @Test
    public void testConvertPositive() {
        String2ShortConvertor converter = new String2ShortConvertor();
        Number result = converter.parse("32767", null, null);
        assertEquals(Short.MAX_VALUE, result);
    }

    @Test
    public void testConvertNegative() {
        String2ShortConvertor converter = new String2ShortConvertor();
        Number result = converter.parse("-32768", null, null);
        assertEquals(Short.MIN_VALUE, result);
    }

    @Test(expected = NumberFormatException.class)
    public void testConvertPositiveOverflow() {
        String2ShortConvertor converter = new String2ShortConvertor();
        converter.parse("32768", null, null);
    }

    @Test(expected = NumberFormatException.class)
    public void testConvertNegativeOverflow() {
        String2ShortConvertor converter = new String2ShortConvertor();
        converter.parse("-32769", null, null);
    }

    @Test(expected = NumberFormatException.class)
    public void testConvertNonInteger() {
        String2ShortConvertor converter = new String2ShortConvertor();
        converter.parse("1.3", null, null);
    }

    @Test
    public void testFormat() {
        String2ShortConvertor converter = new String2ShortConvertor();
        String result = converter.format(Short.MIN_VALUE, null);
        assertEquals("-32768", result);
    }
}
