package org.openl.rules.convertor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class String2ShortConvertorTest {

    @Test
    void testConvertPositive() {
        var converter = new String2ShortConvertor();
        var result = converter.parse("32767", null);
        assertEquals(Short.MAX_VALUE, result);
    }

    @Test
    void testConvertNegative() {
        var converter = new String2ShortConvertor();
        var result = converter.parse("-32768", null);
        assertEquals(Short.MIN_VALUE, result);
    }

    @Test
    void testConvertPositiveOverflow() {
        var converter = new String2ShortConvertor();
        assertThrows(NumberFormatException.class, () -> converter.parse("32768", null));
    }

    @Test
    void testConvertNegativeOverflow() {
        var converter = new String2ShortConvertor();
        assertThrows(NumberFormatException.class, () -> converter.parse("-32769", null));
    }

    @Test
    void testConvertNonInteger() {
        var converter = new String2ShortConvertor();
        assertThrows(NumberFormatException.class, () -> converter.parse("1.3", null));
    }

}
