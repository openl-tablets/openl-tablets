package org.openl.rules.convertor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

    // Overflows on both sides and a non-integer value
    @ParameterizedTest
    @ValueSource(strings = {"32768", "-32769", "1.3"})
    void testConvertInvalid(String value) {
        var converter = new String2ShortConvertor();
        assertThrows(NumberFormatException.class, () -> converter.parse(value, null));
    }

}
