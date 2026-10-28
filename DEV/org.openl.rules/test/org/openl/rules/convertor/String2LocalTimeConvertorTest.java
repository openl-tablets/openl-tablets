package org.openl.rules.convertor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class String2LocalTimeConvertorTest {

    private final String2LocalTimeConvertor converter = new String2LocalTimeConvertor();

    // The US form takes an hour of 1 to 12 with AM or PM
    @ParameterizedTest
    @CsvSource({
            "22:20, 22:20",
            "10:20:30, 10:20:30",
            "10:20 AM, 10:20",
            "10:20 PM, 22:20",
            "09:05 PM, 21:05",
            "9:05 PM, 21:05",
            "12:20 AM, 00:20",
            "12:20 PM, 12:20"})
    void testParse(String value, LocalTime expected) {
        assertEquals(expected, converter.parse(value, null));
    }

    // Empty, not a time, an hour over 12 with PM, an hour 0 with AM and a time without AM or PM
    @ParameterizedTest
    @ValueSource(strings = {"", "Kin-Dza-Dza", "22:20 PM", "0:20 AM", "9:05"})
    void testParseInvalid(String value) {
        var e = assertThrows(IllegalArgumentException.class, () -> converter.parse(value, null));
        assertEquals("Cannot convert '%s' to LocalTime type".formatted(value), e.getMessage());
    }

    @Test
    void testParseNull() {
        assertNull(converter.parse(null, null));
    }

}
