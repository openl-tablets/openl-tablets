package org.openl.rules.convertor;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class String2LongConvertorTest {

    @Test
    public void testConvertPositive() {
        String2LongConvertor converter = new String2LongConvertor();
        Number result = converter.parse("9223372036854775807", null);
        assertEquals(Long.MAX_VALUE, result);
    }

    @Test
    public void testConvertNegative() {
        String2LongConvertor converter = new String2LongConvertor();
        Number result = converter.parse("-9223372036854775808", null);
        assertEquals(Long.MIN_VALUE, result);
    }

    @Test(expected = NumberFormatException.class)
    public void testConvertPositiveOverflow() {
        String2LongConvertor converter = new String2LongConvertor();
        converter.parse("9223372036854775808", null);
    }

    @Test(expected = NumberFormatException.class)
    public void testConvertNegativeOverflow() {
        String2LongConvertor converter = new String2LongConvertor();
        converter.parse("-9223372036854775809", null);
    }

    @Test(expected = NumberFormatException.class)
    public void testConvertNonInteger() {
        String2LongConvertor converter = new String2LongConvertor();
        converter.parse("1.3", null);
    }

}
