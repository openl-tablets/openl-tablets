package org.openl.rules.convertor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class String2InstantConverterTest {

    private final String2InstantConverter converter = new String2InstantConverter();

    // The US form takes a month and a day of one or two digits and an hour of 1 to 12 with AM or PM
    @ParameterizedTest
    @CsvSource({
            "2026-01-01T21:05:00Z, 2026-01-01T21:05:00Z",
            "1/01/2026 10:20 AM UTC, 2026-01-01T10:20:00Z",
            "1/1/2026 9:05 PM Europe/Paris, 2026-01-01T20:05:00Z",
            "1/01/2026 12:20 AM UTC, 2026-01-01T00:20:00Z"})
    void testParse(String value, Instant expected) {
        assertEquals(expected, converter.parse(value, null));
    }

    // Empty, not a date, a date without a zone, an hour over 12 with PM and a day the month does not have
    @ParameterizedTest
    @ValueSource(strings = {"", "Kin-Dza-Dza", "1/01/2026 10:20 AM", "1/01/2026 22:20 PM UTC",
            "2/30/2026 9:05 PM UTC"})
    void testParseInvalid(String value) {
        var e = assertThrows(IllegalArgumentException.class, () -> converter.parse(value, null));
        assertEquals("Cannot convert '%s' to Instant type".formatted(value), e.getMessage());
    }

    @Test
    void testParseNull() {
        assertNull(converter.parse(null, null));
    }

}
