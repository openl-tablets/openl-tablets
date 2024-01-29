package org.openl.rules.convertor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigInteger;

import org.junit.jupiter.api.Test;

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

    @Test
    public void testConvertNonInteger() {
        assertThrows(NumberFormatException.class, () -> {
            String2BigIntegerConvertor converter = new String2BigIntegerConvertor();
            converter.parse("1.3", null);
        });
    }
}
