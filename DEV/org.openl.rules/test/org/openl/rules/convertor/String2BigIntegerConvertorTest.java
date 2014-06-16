package org.openl.rules.convertor;

import static org.junit.Assert.*;

import java.math.BigInteger;

import org.junit.Test;

public class String2BigIntegerConvertorTest {

    @Test
    public void testConvertPositive() {
        String2BigIntegerConvertor converter = new String2BigIntegerConvertor();
        Number result = converter.parse("123456789012345678901234567890", null, null);
        assertEquals(new BigInteger("123456789012345678901234567890"), result);
    }

    @Test
    public void testConvertNegative() {
        String2BigIntegerConvertor converter = new String2BigIntegerConvertor();
        Number result = converter.parse("-12", null, null);
        assertEquals(BigInteger.valueOf(-12L), result);
    }

    @Test(expected = NumberFormatException.class)
    public void testConvertNonInteger() {
        String2BigIntegerConvertor converter = new String2BigIntegerConvertor();
        converter.parse("1.3", null, null);
    }

    @Test
    public void testFormat() {
        String2BigIntegerConvertor converter = new String2BigIntegerConvertor();
        String result = converter.format(new BigInteger("-123456789012345678901234567890"), null);
        assertEquals("-123456789012345678901234567890", result);
    }
}
