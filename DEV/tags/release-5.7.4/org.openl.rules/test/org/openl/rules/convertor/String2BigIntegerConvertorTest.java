package org.openl.rules.convertor;

import static org.junit.Assert.*;

import java.math.BigInteger;

import org.junit.Test;

public class String2BigIntegerConvertorTest {
    
    private static String BIG_INT = "2000000000";
    
    @Test
    public void testParse() {
        String2BigIntegerConvertor conv = new String2BigIntegerConvertor();
        BigInteger res = (BigInteger)conv.parse(BIG_INT, null, null);
        assertEquals(BigInteger.valueOf(2000000000), res);
    }

    @Test
    public void testFormat() {
        BigInteger value = BigInteger.valueOf(2000000000);
        String2BigIntegerConvertor conv = new String2BigIntegerConvertor();
        String formattedValue = conv.format(value, null);
        assertEquals(BIG_INT, formattedValue);
    }
    
    @Test
    public void testParseNull() {
        String2BigIntegerConvertor conv = new String2BigIntegerConvertor();
        assertNull((BigInteger)conv.parse(null, null, null));
    }
    
    @Test
    public void testFormatNull() {
        String2BigIntegerConvertor conv = new String2BigIntegerConvertor();
        assertNull(conv.format(null, null));
    }
}
