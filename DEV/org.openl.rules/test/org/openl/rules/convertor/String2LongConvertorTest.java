package org.openl.rules.convertor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

    // Overflows on both sides, a non-integer value, NaN and an infinity
    @ParameterizedTest
    @ValueSource(strings = {"9223372036854775808", "-9223372036854775809", "1.3", "NaN", "-Infinity"})
    void testConvertInvalid(String value) {
        var converter = new String2LongConvertor();
        assertThrows(NumberFormatException.class, () -> converter.parse(value, null));
    }

}
