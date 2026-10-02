package org.openl.rules.helpers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class DateRangeParsingTest {

    static Stream<Arguments> testToString() {
        return Stream.of(
                arguments("2/11/2020 01:01:1", "02/11/2020 01:01:01"),
                arguments("12/12/2019", "12/12/2019 00:00:00"),
                arguments("03/12/2019", "03/12/2019 00:00:00"),

                arguments("03/12/2019 - 12/01/2019", "[03/12/2019 00:00:00..12/01/2019 00:00:00]"),
                arguments("03/12/2019..12/01/2019", "[03/12/2019 00:00:00..12/01/2019 00:00:00]"),
                arguments("03/12/2019 … 12/01/2019", "(03/12/2019 00:00:00..12/01/2019 00:00:00)"),
                arguments("03/12/2019 ... 12/01/2019", "(03/12/2019 00:00:00..12/01/2019 00:00:00)"),

                arguments("[03/12/2019; 12/01/2019]", "[03/12/2019 00:00:00..12/01/2019 00:00:00]"),
                arguments("(03/12/2019;12/01/2019]", "(03/12/2019 00:00:00..12/01/2019 00:00:00]"),
                arguments("[03/12/2019; 12/01/2019)", "[03/12/2019 00:00:00..12/01/2019 00:00:00)"),
                arguments("(03/12/2019; 12/01/2019)", "(03/12/2019 00:00:00..12/01/2019 00:00:00)"),

                arguments("(03/12/2019 .. 12/01/2019)", "(03/12/2019 00:00:00..12/01/2019 00:00:00)"),
                arguments("[03/12/2019 .. 12/01/2019]", "[03/12/2019 00:00:00..12/01/2019 00:00:00]"),
                arguments("(03/12/2019 .. 12/01/2019]", "(03/12/2019 00:00:00..12/01/2019 00:00:00]"),
                arguments("[03/12/2019 .. 12/01/2019)", "[03/12/2019 00:00:00..12/01/2019 00:00:00)"),

                arguments("03/12/2019 and more", ">= 03/12/2019 00:00:00"),
                arguments("03/12/2019 or less", "<= 03/12/2019 00:00:00"),

                arguments("more than 03/12/2019", "> 03/12/2019 00:00:00"),
                arguments("less than 12/01/2019", "< 12/01/2019 00:00:00"),

                arguments(">= 03/12/2019", ">= 03/12/2019 00:00:00"),
                arguments("<= 03/12/2019", "<= 03/12/2019 00:00:00"),

                arguments("> 03/12/2019", "> 03/12/2019 00:00:00"),
                arguments("< 12/01/2019", "< 12/01/2019 00:00:00"),
                arguments("03/12/2019+", ">= 03/12/2019 00:00:00"),

                arguments(">=03/12/2019 <=12/01/2019", "[03/12/2019 00:00:00..12/01/2019 00:00:00]"),
                arguments("<=12/01/2019 >=03/12/2019", "[03/12/2019 00:00:00..12/01/2019 00:00:00]"),

                arguments(">=03/12/2019 <12/01/2019", "[03/12/2019 00:00:00..12/01/2019 00:00:00)"),
                arguments("<12/01/2019 >=03/12/2019", "[03/12/2019 00:00:00..12/01/2019 00:00:00)"),

                arguments(">03/12/2019 <=12/01/2019", "(03/12/2019 00:00:00..12/01/2019 00:00:00]"),
                arguments("<=12/01/2019 >03/12/2019", "(03/12/2019 00:00:00..12/01/2019 00:00:00]"),

                arguments(">03/12/2019 <12/01/2019", "(03/12/2019 00:00:00..12/01/2019 00:00:00)"),
                arguments("<12/01/2019 >03/12/2019", "(03/12/2019 00:00:00..12/01/2019 00:00:00)"),

                // ISO dates
                arguments("2019-03-12", "03/12/2019 00:00:00"),
                arguments("2019-03-12 00:00:00", "03/12/2019 00:00:00"),
                arguments("2020-2-11 1:1:1", "02/11/2020 01:01:01"),
                arguments("2019-03-12T10:20:30", "03/12/2019 10:20:30"),

                arguments("2019-03-12 - 2019-12-01", "[03/12/2019 00:00:00..12/01/2019 00:00:00]"),
                arguments("2019-03-12-2019-12-01", "[03/12/2019 00:00:00..12/01/2019 00:00:00]"),
                arguments("2019-03-12 00:00:00 - 2019-12-01 23:59:59", "[03/12/2019 00:00:00..12/01/2019 23:59:59]"),
                arguments("2019-03-12 10:00:00-2019-12-01", "[03/12/2019 10:00:00..12/01/2019 00:00:00]"),
                arguments("2019-03-12..2019-12-01", "[03/12/2019 00:00:00..12/01/2019 00:00:00]"),
                arguments("2019-03-12 .. 2019-12-01", "[03/12/2019 00:00:00..12/01/2019 00:00:00]"),
                arguments("2019-03-12 … 2019-12-01", "(03/12/2019 00:00:00..12/01/2019 00:00:00)"),
                arguments("2019-03-12...2019-12-01", "(03/12/2019 00:00:00..12/01/2019 00:00:00)"),
                arguments("2019-03-12; 2019-12-01", "[03/12/2019 00:00:00..12/01/2019 00:00:00]"),

                arguments("[2019-03-12; 2019-12-01]", "[03/12/2019 00:00:00..12/01/2019 00:00:00]"),
                arguments("(2019-03-12;2019-12-01]", "(03/12/2019 00:00:00..12/01/2019 00:00:00]"),
                arguments("[2019-03-12 - 2019-12-01)", "[03/12/2019 00:00:00..12/01/2019 00:00:00)"),
                arguments("( 2019-03-12 .. 2019-12-01 23:59:59 )", "(03/12/2019 00:00:00..12/01/2019 23:59:59)"),
                arguments("[2019-03-12T10:00:00; 2019-12-01T23:59:59]", "[03/12/2019 10:00:00..12/01/2019 23:59:59]"),

                arguments("2019-03-12 and more", ">= 03/12/2019 00:00:00"),
                arguments("2019-12-01 23:59:59 or less", "<= 12/01/2019 23:59:59"),
                arguments("more than 2019-03-12", "> 03/12/2019 00:00:00"),
                arguments("less than 2019-12-01", "< 12/01/2019 00:00:00"),
                arguments(">= 2019-03-12", ">= 03/12/2019 00:00:00"),
                arguments("<=2019-12-01", "<= 12/01/2019 00:00:00"),
                arguments("> 2019-03-12 10:00:00", "> 03/12/2019 10:00:00"),
                arguments("< 2019-12-01", "< 12/01/2019 00:00:00"),
                arguments("2019-03-12+", ">= 03/12/2019 00:00:00"),
                arguments("2019-03-12 00:00:00 +", ">= 03/12/2019 00:00:00"),
                arguments(">=2019-03-12 <2019-12-01", "[03/12/2019 00:00:00..12/01/2019 00:00:00)"),
                arguments("<= 2019-12-01 > 2019-03-12", "(03/12/2019 00:00:00..12/01/2019 00:00:00]"),

                // the US and the ISO forms together
                arguments("03/12/2019 - 2019-12-01", "[03/12/2019 00:00:00..12/01/2019 00:00:00]"),
                arguments("2019-03-12-12/01/2019", "[03/12/2019 00:00:00..12/01/2019 00:00:00]"),
                arguments("[03/12/2019; 2019-12-01)", "[03/12/2019 00:00:00..12/01/2019 00:00:00)"));
    }

    @ParameterizedTest
    @MethodSource
    void testToString(String range, String expected) {
        assertEquals(expected, new DateRange(range).toString());
    }

    @Test
    void testEquals() throws ParseException {
        assertEquals(new DateRange(toDate("03/12/2019 00:00:00"), toDate("03/12/2019 00:00:00")),
                new DateRange("03/12/2019"));
    }

    @Test
    void testIsoEqualsUs() {
        assertEquals(new DateRange("03/12/2019"), new DateRange("2019-03-12"));
        assertEquals(new DateRange("03/12/2019 01:01:22"), new DateRange("2019-03-12T01:01:22"));
        assertEquals(new DateRange("[03/12/2019; 12/01/2019 23:59:59)"),
                new DateRange("[2019-03-12; 2019-12-01 23:59:59)"));
        assertEquals(new DateRange("03/12/2019 and more"), new DateRange(">= 2019-03-12"));
    }

    @Test
    void testIsoFormat() throws ParseException {
        var range = new DateRange("2019-03-12 01:01:22");
        assertInclude(range, "03/12/2019 01:01:22");
        assertExclude(range, "03/12/2019 01:01:23", "03/12/2019 01:01:21");

        range = new DateRange("[2019-03-12; 2019-12-01)");
        assertInclude(range, "03/12/2019 00:00:00", "08/01/2019 23:00:00", "11/30/2019 23:59:59");
        assertExclude(range, "03/11/2019 23:59:59", "12/01/2019 00:00:00");

        range = new DateRange("2019-03-12 .. 2019-12-01 12:00:00");
        assertInclude(range, "03/12/2019 00:00:00", "12/01/2019 12:00:00");
        assertExclude(range, "03/11/2019 23:59:59", "12/01/2019 12:00:01");

        range = new DateRange("less than 2019-12-01");
        assertInclude(range, "03/11/2019 23:59:59", "11/30/2019 23:59:59");
        assertExclude(range, "12/01/2019 00:00:00", "12/01/2019 00:00:01");
    }

    /**
     * A bound is a local date and time, as a date value is, so a range holds the same dates in every time zone.
     */
    @ParameterizedTest
    @ValueSource(strings = {"UTC", "America/Los_Angeles", "Asia/Kolkata", "Pacific/Kiritimati"})
    void testBoundIsLocalTime(String zone) throws ParseException {
        var defaultZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone(zone));
        try {
            var range = new DateRange("[2024-12-25; 12/26/2024 12:30:00)");
            assertInclude(range, "12/25/2024 00:00:00", "12/26/2024 12:29:59");
            assertExclude(range, "12/24/2024 23:59:59", "12/26/2024 12:30:00");
            assertEquals("[12/25/2024 00:00:00..12/26/2024 12:30:00)", range.toString());
        } finally {
            TimeZone.setDefault(defaultZone);
        }
    }

    static Stream<Arguments> testLikelyRangeThanDate() {
        return Stream.of(
                arguments("[2024-01-01; 2024-07-01)", true),
                arguments("2024-01-01 - 2024-12-31 23:59:59", true),
                arguments("2024-01-01..2024-12-31", true),
                arguments(">= 2024-01-01T10:00:00", true),
                arguments("2024-01-01 and more", true),
                arguments("01/01/2024 - 2024-12-31", true),
                arguments("01/01/2024 - 12/31/2024", true),
                arguments("2024-12-25", false),
                arguments("2024-12-25 00:00:00", false),
                arguments("12/25/2024", false),
                arguments("1 - 5", false),
                arguments("2024-12 - 2025-01", false),
                arguments("[2024-01-01; 2024-07-01", false));
    }

    @ParameterizedTest
    @MethodSource
    void testLikelyRangeThanDate(String value, boolean expected) {
        assertEquals(expected, DateRangeParser.getInstance().likelyRangeThanDate(value));
    }

    @Test
    void testSimpleRangeFormat() throws ParseException {
        var range = new DateRange("03/12/2019");
        assertInclude(range, "03/12/2019 00:00:00");
        assertExclude(range, "03/12/2019 00:00:01", "03/11/2019 23:59:59");

        range = new DateRange("03/12/2019 01:01:22");
        assertInclude(range, "03/12/2019 01:01:22");
        assertExclude(range, "03/12/2019 01:01:23", "03/11/2019 01:01:21");

        range = new DateRange("3/2/2019 1:1:2");
        assertInclude(range, "03/02/2019 01:01:02");
        assertExclude(range, "03/02/2019 01:01:03", "03/11/2019 01:01:01");
    }

    @Test
    void testMinMaxRangeFormat() throws ParseException {
        var range = new DateRange("03/12/2019 - 12/01/2019");
        assertInclude(range,
                "03/12/2019 00:00:00",
                "03/12/2019 00:00:01",
                "08/01/2019 23:00:00",
                "11/30/2019 23:59:59",
                "12/01/2019 00:00:00");
        assertExclude(range, "03/11/2019 23:59:59", "12/01/2019 00:00:01");

        range = new DateRange("03/12/2019 .. 12/01/2019");
        assertInclude(range,
                "03/12/2019 00:00:00",
                "03/12/2019 00:00:01",
                "08/01/2019 23:00:00",
                "11/30/2019 23:59:59",
                "12/01/2019 00:00:00");
        assertExclude(range, "03/11/2019 23:59:59", "12/01/2019 00:00:01");

        range = new DateRange("03/12/2019 ... 12/01/2019");
        assertInclude(range, "03/12/2019 00:00:01", "08/01/2019 23:00:00", "11/30/2019 23:59:59");
        assertExclude(range,
                "03/11/2019 23:59:59",
                "12/01/2019 00:00:01",
                "03/12/2019 00:00:00",
                "12/01/2019 00:00:00");

        range = new DateRange("03/12/2019 ... 12/01/2019");
        assertInclude(range, "03/12/2019 00:00:01", "08/01/2019 23:00:00", "11/30/2019 23:59:59");
        assertExclude(range,
                "03/11/2019 23:59:59",
                "12/01/2019 00:00:01",
                "03/12/2019 00:00:00",
                "12/01/2019 00:00:00");
    }

    @Test
    void testVerbal() throws ParseException {
        var range = new DateRange("03/12/2019 and more");
        assertInclude(range,
                "03/12/2019 00:00:00",
                "03/12/2019 00:00:01",
                "08/01/2019 23:00:00",
                "11/30/2019 23:59:59",
                "12/01/2019 00:00:00",
                "12/01/2019 00:00:01");
        assertExclude(range, "03/11/2019 23:59:59");

        range = new DateRange("12/01/2019 or less");
        assertInclude(range,
                "03/12/2019 00:00:00",
                "03/12/2019 00:00:01",
                "08/01/2019 23:00:00",
                "11/30/2019 23:59:59",
                "12/01/2019 00:00:00",
                "03/11/2019 23:59:59");
        assertExclude(range, "12/01/2019 00:00:01");

        range = new DateRange("more than 03/12/2019");
        assertInclude(range,
                "03/12/2019 00:00:01",
                "08/01/2019 23:00:00",
                "11/30/2019 23:59:59",
                "12/01/2019 00:00:00",
                "12/01/2019 00:00:01");
        assertExclude(range, "03/11/2019 23:59:59", "03/12/2019 00:00:00");

        range = new DateRange("less than 12/01/2019");
        assertInclude(range,
                "03/12/2019 00:00:00",
                "03/12/2019 00:00:01",
                "08/01/2019 23:00:00",
                "11/30/2019 23:59:59",
                "03/11/2019 23:59:59");
        assertExclude(range, "12/01/2019 00:00:01", "12/01/2019 00:00:00");
    }

    @Test
    void testMoreLessFormat() throws ParseException {
        var range = new DateRange(">= 03/12/2019");
        assertInclude(range,
                "03/12/2019 00:00:00",
                "03/12/2019 00:00:01",
                "08/01/2019 23:00:00",
                "11/30/2019 23:59:59",
                "12/01/2019 00:00:00",
                "12/01/2019 00:00:01");
        assertExclude(range, "03/11/2019 23:59:59");

        range = new DateRange("<= 12/01/2019");
        assertInclude(range,
                "03/12/2019 00:00:00",
                "03/12/2019 00:00:01",
                "08/01/2019 23:00:00",
                "11/30/2019 23:59:59",
                "12/01/2019 00:00:00",
                "03/11/2019 23:59:59");
        assertExclude(range, "12/01/2019 00:00:01");

        range = new DateRange("> 03/12/2019");
        assertInclude(range,
                "03/12/2019 00:00:01",
                "08/01/2019 23:00:00",
                "11/30/2019 23:59:59",
                "12/01/2019 00:00:00",
                "12/01/2019 00:00:01");
        assertExclude(range, "03/11/2019 23:59:59", "03/12/2019 00:00:00");

        range = new DateRange("< 12/01/2019");
        assertInclude(range,
                "03/12/2019 00:00:00",
                "03/12/2019 00:00:01",
                "08/01/2019 23:00:00",
                "11/30/2019 23:59:59",
                "03/11/2019 23:59:59");
        assertExclude(range, "12/01/2019 00:00:01", "12/01/2019 00:00:00");

        range = new DateRange("03/12/2019 +");
        assertInclude(range,
                "03/12/2019 00:00:00",
                "03/12/2019 00:00:01",
                "08/01/2019 23:00:00",
                "11/30/2019 23:59:59",
                "12/01/2019 00:00:00",
                "12/01/2019 00:00:01");
        assertExclude(range, "03/11/2019 23:59:59");
    }

    @Test
    void testBracketsFormat() throws ParseException {
        var range = new DateRange("[03/12/2019; 12/01/2019]");
        assertInclude(range,
                "03/12/2019 00:00:00",
                "03/12/2019 00:00:01",
                "08/01/2019 23:00:00",
                "11/30/2019 23:59:59",
                "12/01/2019 00:00:00");
        assertExclude(range, "03/11/2019 23:59:59", "12/01/2019 00:00:01");

        range = new DateRange("[03/12/2019; 12/01/2019)");
        assertInclude(range,
                "03/12/2019 00:00:00",
                "03/12/2019 00:00:01",
                "08/01/2019 23:00:00",
                "11/30/2019 23:59:59");
        assertExclude(range, "03/11/2019 23:59:59", "12/01/2019 00:00:01", "12/01/2019 00:00:00");

        range = new DateRange("(03/12/2019; 12/01/2019]");
        assertInclude(range,
                "03/12/2019 00:00:01",
                "08/01/2019 23:00:00",
                "11/30/2019 23:59:59",
                "12/01/2019 00:00:00");
        assertExclude(range, "03/11/2019 23:59:59", "12/01/2019 00:00:01", "03/12/2019 00:00:00");

        range = new DateRange("(03/12/2019; 12/01/2019)");
        assertInclude(range, "03/12/2019 00:00:01", "08/01/2019 23:00:00", "11/30/2019 23:59:59");
        assertExclude(range,
                "03/11/2019 23:59:59",
                "12/01/2019 00:00:01",
                "03/12/2019 00:00:00",
                "12/01/2019 00:00:00");
    }

    @Test
    void testMoreLessFormatBothBounds() throws ParseException {
        var range = new DateRange(">=03/12/2019 <=12/01/2019");
        assertInclude(range,
                "03/12/2019 00:00:00",
                "03/12/2019 00:00:01",
                "08/01/2019 23:00:00",
                "11/30/2019 23:59:59",
                "12/01/2019 00:00:00");
        assertExclude(range, "03/11/2019 23:59:59", "12/01/2019 00:00:01");

        range = new DateRange("<=12/01/2019 >=03/12/2019");
        assertInclude(range,
                "03/12/2019 00:00:00",
                "03/12/2019 00:00:01",
                "08/01/2019 23:00:00",
                "11/30/2019 23:59:59",
                "12/01/2019 00:00:00");
        assertExclude(range, "03/11/2019 23:59:59", "12/01/2019 00:00:01");

        range = new DateRange(">=03/12/2019 <12/01/2019");
        assertInclude(range,
                "03/12/2019 00:00:00",
                "03/12/2019 00:00:01",
                "08/01/2019 23:00:00",
                "11/30/2019 23:59:59");
        assertExclude(range, "03/11/2019 23:59:59", "12/01/2019 00:00:01", "12/01/2019 00:00:00");

        range = new DateRange("<12/01/2019 >=03/12/2019");
        assertInclude(range,
                "03/12/2019 00:00:00",
                "03/12/2019 00:00:01",
                "08/01/2019 23:00:00",
                "11/30/2019 23:59:59");
        assertExclude(range, "03/11/2019 23:59:59", "12/01/2019 00:00:01", "12/01/2019 00:00:00");

        range = new DateRange(">03/12/2019 <=12/01/2019");
        assertInclude(range,
                "03/12/2019 00:00:01",
                "08/01/2019 23:00:00",
                "11/30/2019 23:59:59",
                "12/01/2019 00:00:00");
        assertExclude(range, "03/11/2019 23:59:59", "12/01/2019 00:00:01", "03/12/2019 00:00:00");

        range = new DateRange("<=12/01/2019 >03/12/2019");
        assertInclude(range,
                "03/12/2019 00:00:01",
                "08/01/2019 23:00:00",
                "11/30/2019 23:59:59",
                "12/01/2019 00:00:00");
        assertExclude(range, "03/11/2019 23:59:59", "12/01/2019 00:00:01", "03/12/2019 00:00:00");

        range = new DateRange(">03/12/2019 <12/01/2019");
        assertInclude(range, "03/12/2019 00:00:01", "08/01/2019 23:00:00", "11/30/2019 23:59:59");
        assertExclude(range,
                "03/11/2019 23:59:59",
                "12/01/2019 00:00:01",
                "03/12/2019 00:00:00",
                "12/01/2019 00:00:00");

        range = new DateRange("<12/01/2019 >03/12/2019");
        assertInclude(range, "03/12/2019 00:00:01", "08/01/2019 23:00:00", "11/30/2019 23:59:59");
        assertExclude(range,
                "03/11/2019 23:59:59",
                "12/01/2019 00:00:01",
                "03/12/2019 00:00:00",
                "12/01/2019 00:00:00");
    }

    @Test
    void testNulls() {
        var range = new DateRange("<12/01/2019 >03/12/2019");
        assertFalse(range.contains((Date) null));
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"foo", "3/2/2019 1:1:", "3/2/2019 1:1", "3/2/2019 1:", "3/2/2019 1", "3/2/", "3/2", "3/",
            "3", "", "3/2/2019 1:1:1 sdsdsd", "sdsdsd 3/2/2019 1:1:1", "2/11/2020 99:99:99", "2/99/2020 12:1:1",
            "2019-03", "19-03-12", "2019-13-12", "2019-03-32", "2019-03-12 25:00:00", "2019-03-12 10:00", "2019-03-12T",
            "2019-03-12 T10:00:00", "12/03/2019T10:00:00", "2019-03-12 - 2019-03", "2019-12-01 - 2019-03-12",
            "[2019-03-12; 2019-12-01", "[2019-03-12]"})
    void testNegativeCases(String range) {
        assertThrows(RuntimeException.class, () -> new DateRange(range));
    }

    private void assertInclude(DateRange range, String... args) throws ParseException {
        assertNotNull(range);
        assertTrue(args.length > 0);
        for (String s : args) {
            assertTrue(range.contains(toDate(s)),
                    "The range %s must include a date '%s'".formatted(range.toString(), s));
        }
    }

    private void assertExclude(DateRange range, String... args) throws ParseException {
        assertNotNull(range);
        assertTrue(args.length > 0);
        for (String s : args) {
            assertFalse(range.contains(toDate(s)),
                    "The range %s must not include a date '%s'".formatted(range.toString(), s));
        }
    }

    private Date toDate(String s) throws ParseException {
        var formatter = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss", Locale.US);
        formatter.setLenient(false); // Strict matching
        formatter.getCalendar().set(0, 0, 0, 0, 0, 0); // at
        formatter.getCalendar().set(Calendar.MILLISECOND, 0);
        return formatter.parse(s);
    }

}
