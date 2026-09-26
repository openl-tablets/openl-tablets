package org.openl.rules.convertor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class String2LongConvertorTest {

    @Test
    void testConvertPositive() {
        var converter = new String2LongConvertor();
        var result = converter.parse("9223372036854775807", null);
        assertEquals(Long.MAX_VALUE, result);
    }

    @Test
    void testConvertNegative() {
        var converter = new String2LongConvertor();
        var result = converter.parse("-9223372036854775808", null);
        assertEquals(Long.MIN_VALUE, result);
    }

    @Test
    void testConvertPositiveOverflow() {
        var converter = new String2LongConvertor();
        assertThrows(NumberFormatException.class, () -> converter.parse("9223372036854775808", null));
    }

    @Test
    void testConvertNegativeOverflow() {
        var converter = new String2LongConvertor();
        assertThrows(NumberFormatException.class, () -> converter.parse("-9223372036854775809", null));
    }

    @Test
    void testConvertNonInteger() {
        var converter = new String2LongConvertor();
        assertThrows(NumberFormatException.class, () -> converter.parse("1.3", null));
    }

}
