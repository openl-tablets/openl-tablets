package org.openl.rules.convertor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class String2LocalDateTimeConvertorTest {

    private final String2LocalDateTimeConvertor converter = new String2LocalDateTimeConvertor();

    @ParameterizedTest
    @CsvSource({
            "2026-01-01T10:20, 2026-01-01T10:20",
            "2026-01-01T10:20:30.5, 2026-01-01T10:20:30.5",
            "1/01/2026 10:20 AM, 2026-01-01T10:20",
            // A date without a time is the start of that day
            "2026-01-01, 2026-01-01T00:00",
            "12/31/2026, 2026-12-31T00:00"})
    void testParse(String value, LocalDateTime expected) {
        assertEquals(expected, converter.parse(value, null));
    }

    // Empty, not a date, an unfinished time, a misprint and a date with a text after it
    @ParameterizedTest
    @ValueSource(strings = {"", "Kin-Dza-Dza", "2026-01-01T", "2026-13-01", "2026-01-01 abc"})
    void testParseInvalid(String value) {
        var e = assertThrows(IllegalArgumentException.class, () -> converter.parse(value, null));
        assertEquals("Cannot convert '%s' to LocalDateTime type".formatted(value), e.getMessage());
    }

    @Test
    void testParseNull() {
        assertNull(converter.parse(null, null));
    }

}
