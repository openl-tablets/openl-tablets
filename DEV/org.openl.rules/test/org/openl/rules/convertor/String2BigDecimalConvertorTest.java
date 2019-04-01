package org.openl.rules.convertor;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;

import org.junit.Test;

public class String2BigDecimalConvertorTest {

    @Test
    public void testConvertPositive() {
        String2BigDecimalConvertor converter = new String2BigDecimalConvertor();
        Number result = converter.parse("1234.56789012345678901234567890", null);
        assertEquals(new BigDecimal("1234.5678901234567890123456789"), result);
    }

    @Test
    public void testConvertNegative() {
        String2BigDecimalConvertor converter = new String2BigDecimalConvertor();
        Number result = converter.parse("-12", null);
        assertEquals(BigDecimal.valueOf(-12L), result);
    }

    @Test
    public void testConvertWithZeroPrecision() {
        String2BigDecimalConvertor converter = new String2BigDecimalConvertor();
        Number result = converter.parse("4.00", null);
        assertEquals(BigDecimal.valueOf(4), result);
    }

    @Test
    public void testConvertWithoutZeroPrecision() {
        String2BigDecimalConvertor converter = new String2BigDecimalConvertor();
        Number result = converter.parse("4", null);
        assertEquals(BigDecimal.valueOf(4), result);
    }
}
