package org.openl.rules.convertor;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class String2ShortConvertorTest {

    @Test
    public void testConvertPositive() {
        String2ShortConvertor converter = new String2ShortConvertor();
        Number result = converter.parse("32767", null);
        assertEquals(Short.MAX_VALUE, result);
    }

    @Test
    public void testConvertNegative() {
        String2ShortConvertor converter = new String2ShortConvertor();
        Number result = converter.parse("-32768", null);
        assertEquals(Short.MIN_VALUE, result);
    }

    @Test(expected = NumberFormatException.class)
    public void testConvertPositiveOverflow() {
        String2ShortConvertor converter = new String2ShortConvertor();
        converter.parse("32768", null);
    }

    @Test(expected = NumberFormatException.class)
    public void testConvertNegativeOverflow() {
        String2ShortConvertor converter = new String2ShortConvertor();
        converter.parse("-32769", null);
    }

    @Test(expected = NumberFormatException.class)
    public void testConvertNonInteger() {
        String2ShortConvertor converter = new String2ShortConvertor();
        converter.parse("1.3", null);
    }

}
