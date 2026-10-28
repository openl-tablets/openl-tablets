package org.openl.rules.convertor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class String2LocalDateConvertorTest {

    private final String2LocalDateConvertor converter = new String2LocalDateConvertor();

    // The US form takes a month and a day of one or two digits
    @ParameterizedTest
    @CsvSource({
            "2026-01-01, 2026-01-01",
            "12/31/2026, 2026-12-31",
            "1/01/2026, 2026-01-01",
            "5/7/1981, 1981-05-07",
            "05/07/1981, 1981-05-07",
            "2/29/2024, 2024-02-29"})
    void testParse(String value, LocalDate expected) {
        assertEquals(expected, converter.parse(value, null));
    }

    // Empty, not a date, a time after the date, a misprint and a two-digit year.
    // A day the month does not have is refused, not moved to the last day: 2/30/2026 is not 2026-02-28.
    @ParameterizedTest
    @ValueSource(strings = {"", "Kin-Dza-Dza", "2026-01-01T10:20", "2026-13-01", "5/7/81",
            "2026-02-30", "2/30/2026", "2/29/2026", "4/31/2026"})
    void testParseInvalid(String value) {
        var e = assertThrows(IllegalArgumentException.class, () -> converter.parse(value, null));
        assertEquals("Cannot convert '%s' to LocalDate type".formatted(value), e.getMessage());
    }

    @Test
    void testParseNull() {
        assertNull(converter.parse(null, null));
    }

}
