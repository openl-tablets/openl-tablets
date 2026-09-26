package org.openl.rules.convertor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class String2IntConvertorTest {

    @Test
    void testConvertPositive() {
        var converter = new String2IntConvertor();
        var result = converter.parse("2147483647", null);
        assertEquals(Integer.MAX_VALUE, result);
    }

    @Test
    void testConvertNegative() {
        var converter = new String2IntConvertor();
        var result = converter.parse("-2147483648", null);
        assertEquals(Integer.MIN_VALUE, result);
    }

    @Test
    void testConvertPositiveOverflow() {
        var converter = new String2IntConvertor();
        assertThrows(NumberFormatException.class, () -> converter.parse("2147483648", null));
    }

    @Test
    void testConvertNegativeOverflow() {
        var converter = new String2IntConvertor();
        assertThrows(NumberFormatException.class, () -> converter.parse("-2147483649", null));
    }

    @Test
    void testConvertNonInteger() {
        var converter = new String2IntConvertor();
        assertThrows(NumberFormatException.class, () -> converter.parse("1.3", null));
    }

}
