package org.openl.rules.convertor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class String2LongConvertorTest {

    @Test
    void testConvertPositive() {
        String2LongConvertor converter = new String2LongConvertor();
        Number result = converter.parse("9223372036854775807", null);
        assertEquals(Long.MAX_VALUE, result);
    }

    @Test
    void testConvertNegative() {
        String2LongConvertor converter = new String2LongConvertor();
        Number result = converter.parse("-9223372036854775808", null);
        assertEquals(Long.MIN_VALUE, result);
    }

    @Test
    void testConvertPositiveOverflow() {
        assertThrows(NumberFormatException.class, () -> {
            String2LongConvertor converter = new String2LongConvertor();
            converter.parse("9223372036854775808", null);
        });
    }

    @Test
    void testConvertNegativeOverflow() {
        assertThrows(NumberFormatException.class, () -> {
            String2LongConvertor converter = new String2LongConvertor();
            converter.parse("-9223372036854775809", null);
        });
    }

    @Test
    void testConvertNonInteger() {
        assertThrows(NumberFormatException.class, () -> {
            String2LongConvertor converter = new String2LongConvertor();
            converter.parse("1.3", null);
        });
    }

}
