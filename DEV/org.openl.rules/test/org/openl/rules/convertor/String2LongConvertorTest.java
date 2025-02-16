package org.openl.rules.convertor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class String2LongConvertorTest {

    @Test
    public void testConvertPositive() {
        String2LongConvertor converter = new String2LongConvertor();
        Number result = converter.parse("9223372036854775807", null);
        assertEquals(Long.MAX_VALUE, result);
    }

    @Test
    public void testConvertNegative() {
        String2LongConvertor converter = new String2LongConvertor();
        Number result = converter.parse("-9223372036854775808", null);
        assertEquals(Long.MIN_VALUE, result);
    }

    @Test
    public void testConvertPositiveOverflow() {
        assertThrows(NumberFormatException.class, () -> {
            String2LongConvertor converter = new String2LongConvertor();
            converter.parse("9223372036854775808", null);
        });
    }

    @Test
    public void testConvertNegativeOverflow() {
        assertThrows(NumberFormatException.class, () -> {
            String2LongConvertor converter = new String2LongConvertor();
            converter.parse("-9223372036854775809", null);
        });
    }

    @Test
    public void testConvertNonInteger() {
        assertThrows(NumberFormatException.class, () -> {
            String2LongConvertor converter = new String2LongConvertor();
            converter.parse("1.3", null);
        });
    }

}
