package org.openl.rules.convertor;

import org.junit.Test;

import java.math.BigDecimal;

import static org.junit.Assert.assertEquals;

public class String2BigDecimalConvertorTest {

    @Test
    public void testConvertPositive() {
        String2BigDecimalConvertor converter = new String2BigDecimalConvertor();
        Number result = converter.parse("1234.56789012345678901234567890", null);
        assertEquals(new BigDecimal("1234.56789012345678901234567890"), result);
    }

    @Test
    public void testConvertNegative() {
        String2BigDecimalConvertor converter = new String2BigDecimalConvertor();
        Number result = converter.parse("-12", null);
        assertEquals(BigDecimal.valueOf(-12L), result);
    }
}
