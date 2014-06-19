package org.openl.rules.convertor;

import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;

import static org.junit.Assert.assertEquals;

public class String2BigDecimalConvertorTest {

    @Test
    public void testConvertPositive() {
        String2BigDecimalConvertor converter = new String2BigDecimalConvertor();
        Number result = converter.parse("1234.56789012345678901234567890", null, null);
        assertEquals(new BigDecimal("1234.56789012345678901234567890"), result);
    }

    @Test
    public void testConvertNegative() {
        String2BigDecimalConvertor converter = new String2BigDecimalConvertor();
        Number result = converter.parse("-12", null, null);
        assertEquals(BigDecimal.valueOf(-12L), result);
    }

    @Test
    public void testFormat() {
        String2BigDecimalConvertor converter = new String2BigDecimalConvertor();
        String result = converter.format(new BigDecimal("-1234.56789012345678901234567890"), null);
        assertEquals("-1234.5678901234567890123456789", result);
    }

    @Test
    public void testFormatZero() {
        String2BigDecimalConvertor converter = new String2BigDecimalConvertor();
        String result = converter.format(BigDecimal.ZERO, null);
        assertEquals("0.0", result);
    }
}
