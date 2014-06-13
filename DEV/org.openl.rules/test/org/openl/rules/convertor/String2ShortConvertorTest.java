package org.openl.rules.convertor;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class String2ShortConvertorTest {

    @Test
    public void testConvertPositive() {
        String2ShortConvertor conv = new String2ShortConvertor();
        Number result = conv.convert(Short.MAX_VALUE, "");
        assertEquals(Short.MAX_VALUE, result);
    }

    @Test
    public void testConvertNegative() {
        String2ShortConvertor conv = new String2ShortConvertor();
        Number result = conv.convert(Short.MIN_VALUE, "");
        assertEquals(Short.MIN_VALUE, result);
    }

    @Test(expected = NumberFormatException.class)
    public void testConvertPositiveOverflow() {
        String2ShortConvertor conv = new String2ShortConvertor();
        conv.convert(Short.MAX_VALUE + 1L, "");
    }

    @Test(expected = NumberFormatException.class)
    public void testConvertNegativeOverflow() {
        String2ShortConvertor conv = new String2ShortConvertor();
        conv.convert(Short.MIN_VALUE - 1L, "");
    }
}
