package org.openl.rules.convertor;

import org.junit.Test;

import java.math.BigInteger;

import static org.junit.Assert.assertEquals;

public class String2BigIntegerConvertorTest {

    @Test
    public void testConvertPositive() {
        String2BigIntegerConvertor converter = new String2BigIntegerConvertor();
        Number result = converter.parse("123456789012345678901234567890", null);
        assertEquals(new BigInteger("123456789012345678901234567890"), result);
    }

    @Test
    public void testConvertNegative() {
        String2BigIntegerConvertor converter = new String2BigIntegerConvertor();
        Number result = converter.parse("-12", null);
        assertEquals(BigInteger.valueOf(-12L), result);
    }

    @Test(expected = NumberFormatException.class)
    public void testConvertNonInteger() {
        String2BigIntegerConvertor converter = new String2BigIntegerConvertor();
        converter.parse("1.3", null);
    }
}
