package org.openl.rules.convertor;

import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

public class String2DoubleConvertorTest {

    @Test
    public void testConvertPositive() {
        String2DoubleConvertor conv = new String2DoubleConvertor();
        Number result = conv.convert(123.125f, "");
        assertEquals(123.125d, result);
    }

    @Test
    public void testConvertNegative() {
        String2DoubleConvertor conv = new String2DoubleConvertor();
        Number result = conv.convert(-123.125f, "");
        assertEquals(-123.125d, result);
    }

    @Test
    public void testConvertPositiveOverflow() {
        String2DoubleConvertor conv = new String2DoubleConvertor();
        Number result = conv.convert(BigDecimal.valueOf(Double.MAX_VALUE).multiply(BigDecimal.TEN), "");
        assertEquals(Double.POSITIVE_INFINITY, result);
    }

    @Test
    public void testConvertNegativeOverflow() {
        String2DoubleConvertor conv = new String2DoubleConvertor();
        Number result = conv.convert(BigDecimal.valueOf(-Double.MAX_VALUE).multiply(BigDecimal.TEN), "");
        assertEquals(Double.NEGATIVE_INFINITY, result);
    }
}
