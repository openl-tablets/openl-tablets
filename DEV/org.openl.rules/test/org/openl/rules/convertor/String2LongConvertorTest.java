package org.openl.rules.convertor;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class String2LongConvertorTest {

    @Test
    public void testConvertPositive() {
        String2LongConvertor conv = new String2LongConvertor();
        Number result = conv.convert(Long.MAX_VALUE, "");
        assertEquals(Long.MAX_VALUE, result);
    }

    @Test
    public void testConvertNegative() {
        String2LongConvertor conv = new String2LongConvertor();
        Number result = conv.convert(Long.MIN_VALUE, "");
        assertEquals(Long.MIN_VALUE, result);
    }

    @Test(expected = NumberFormatException.class)
    public void testConvertPositiveOverflow() {
        String2LongConvertor conv = new String2LongConvertor();
        conv.convert(1e39d, "");
    }

    @Test(expected = NumberFormatException.class)
    public void testConvertNegativeOverflow() {
        String2LongConvertor conv = new String2LongConvertor();
        conv.convert(-1e39d, "");
    }
}
