package org.openl.rules.convertor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class String2ShortConvertorTest {

    @Test
    void testConvertPositive() {
        String2ShortConvertor converter = new String2ShortConvertor();
        Number result = converter.parse("32767", null);
        assertEquals(Short.MAX_VALUE, result);
    }

    @Test
    void testConvertNegative() {
        String2ShortConvertor converter = new String2ShortConvertor();
        Number result = converter.parse("-32768", null);
        assertEquals(Short.MIN_VALUE, result);
    }

    @Test
    void testConvertPositiveOverflow() {
        assertThrows(NumberFormatException.class, () -> {
            String2ShortConvertor converter = new String2ShortConvertor();
            converter.parse("32768", null);
        });
    }

    @Test
    void testConvertNegativeOverflow() {
        assertThrows(NumberFormatException.class, () -> {
            String2ShortConvertor converter = new String2ShortConvertor();
            converter.parse("-32769", null);
        });
    }

    @Test
    void testConvertNonInteger() {
        assertThrows(NumberFormatException.class, () -> {
            String2ShortConvertor converter = new String2ShortConvertor();
            converter.parse("1.3", null);
        });
    }

}
