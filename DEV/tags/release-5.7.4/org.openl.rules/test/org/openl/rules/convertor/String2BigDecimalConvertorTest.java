package org.openl.rules.convertor;

import static org.junit.Assert.*;

import java.math.BigDecimal;

import org.junit.Test;

public class String2BigDecimalConvertorTest {    
    
    private static String DECIMAL = "1115.37";
    
    @Test
    public void testParse() {
        String2BigDecimalConvertor conv = new String2BigDecimalConvertor();
        BigDecimal res = (BigDecimal)conv.parse(DECIMAL, null, null);
        assertEquals(BigDecimal.valueOf(1115.37), res);
    }

    @Test
    public void testFormat() {
        BigDecimal value = BigDecimal.valueOf(1115.37);
        String2BigDecimalConvertor conv = new String2BigDecimalConvertor();
        String formattedValue = conv.format(value, null);
        assertEquals(DECIMAL, formattedValue);
    }
    
    @Test
    public void testParseNull() {
        String2BigDecimalConvertor conv = new String2BigDecimalConvertor();
        assertNull((BigDecimal)conv.parse(null, null, null));
    }
    
    @Test
    public void testFormatNull() {
        String2BigDecimalConvertor conv = new String2BigDecimalConvertor();
        assertNull(conv.format(null, null));
    }

}
