package org.openl.rules.convertor;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class String2IntConvertorTest {

    @Test
    public void testConvertPositive() {
        String2IntConvertor conv = new String2IntConvertor();
        Number result = conv.convert(Integer.MAX_VALUE, "");
        assertEquals(Integer.MAX_VALUE, result);
    }

    @Test
    public void testConvertNegative() {
        String2IntConvertor conv = new String2IntConvertor();
        Number result = conv.convert(Integer.MIN_VALUE, "");
        assertEquals(Integer.MIN_VALUE, result);
    }

    @Test(expected = NumberFormatException.class)
    public void testConvertPositiveOverflow() {
        String2IntConvertor conv = new String2IntConvertor();
        conv.convert(Integer.MAX_VALUE + 1L, "");
    }

    @Test(expected = NumberFormatException.class)
    public void testConvertNegativeOverflow() {
        String2IntConvertor conv = new String2IntConvertor();
        conv.convert(Integer.MIN_VALUE - 1L, "");
    }
}
