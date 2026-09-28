package org.openl.rules.convertor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

    // Overflows on both sides and a non-integer value
    @ParameterizedTest
    @ValueSource(strings = {"2147483648", "-2147483649", "1.3"})
    void testConvertInvalid(String value) {
        var converter = new String2IntConvertor();
        assertThrows(NumberFormatException.class, () -> converter.parse(value, null));
    }

}
