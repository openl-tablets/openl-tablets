package org.openl.rules.convertor;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class String2ByteConvertorTest {

    @Test
    public void testConvertPositive() {
        String2ByteConvertor conv = new String2ByteConvertor();
        Number result = conv.convert(Byte.MAX_VALUE, "");
        assertEquals(Byte.MAX_VALUE, result);
    }

    @Test
    public void testConvertNegative() {
        String2ByteConvertor conv = new String2ByteConvertor();
        Number result = conv.convert(Byte.MIN_VALUE, "");
        assertEquals(Byte.MIN_VALUE, result);
    }

    @Test(expected = NumberFormatException.class)
    public void testConvertPositiveOverflow() {
        String2ByteConvertor conv = new String2ByteConvertor();
        conv.convert(Byte.MAX_VALUE + 1L, "");
    }

    @Test(expected = NumberFormatException.class)
    public void testConvertNegativeOverflow() {
        String2ByteConvertor conv = new String2ByteConvertor();
        conv.convert(Byte.MIN_VALUE - 1L, "");
    }
}
