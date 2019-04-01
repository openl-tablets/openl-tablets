package org.openl.rules.convertor;

import static org.junit.Assert.assertEquals;

import java.math.BigDecimal;

import org.junit.Before;
import org.junit.Test;
import org.openl.meta.BigDecimalValue;

public class String2BigDecimalValueConverterTest {

    private String2BigDecimalValueConverter converter;

    @Before
    public void setUp() {
        converter = new String2BigDecimalValueConverter();
    }

    @Test
    public void testConvertPositive() {
        Number result = converter.parse("1234.56789012345678901234567890", null);
        assertEquals(new BigDecimalValue("1234.5678901234567890123456789"), result);
    }

    @Test
    public void testConvertNegative() {
        Number result = converter.parse("-12", null);
        assertEquals(new BigDecimalValue(BigDecimal.valueOf(-12L)), result);
    }

    @Test
    public void testConvertWithZeroPrecision() {
        Number result = converter.parse("4.00", null);
        assertEquals(new BigDecimalValue(BigDecimal.valueOf(4)), result);
    }

    @Test
    public void testConvertWithoutZeroPrecision() {
        Number result = converter.parse("4", null);
        assertEquals(new BigDecimalValue(BigDecimal.valueOf(4)), result);
    }

}
