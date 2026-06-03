package org.openl.rules.serialization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class ExtendedStdDateFormatTest {

    private ExtendedStdDateFormat df;

    @BeforeEach
    public void setUp() {
        df = new ExtendedStdDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");
    }

    @Test
    public void testFormat() {
        testFormat(df);
        testFormat(df.clone());
    }

    private void testFormat(DateFormat df) {
        Date date = createDate(2018, 1, 1, 3, 4, 5);
        final String actual = df.format(date);
        final String expected = "2018-01-01T03:04:05.000";
        assertEquals(expected, actual);
    }

    @Test
    public void testParse_defaultImplementation() throws ParseException {
        testParse_defaultImplementation(df);
        testParse_defaultImplementation(df.clone());
    }

    private void testParse_defaultImplementation(DateFormat df) throws ParseException {
        final Date actual = df.parse("2018-02-03");
        final Date expected = createDate(2018, 2, 3, 0, 0, 0);
        assertEquals(expected, actual);
    }

    @Test
    public void testParse_customDateFormat() throws ParseException {
        testParse_customDateFormat(df);
        testParse_customDateFormat(df.clone());
    }

    private void testParse_customDateFormat(DateFormat df) throws ParseException {
        final Date actual = df.parse("2019-01-01T03:04:05.000");
        final Date expected = createDate(2019, 1, 1, 3, 4, 5);
        assertEquals(expected, actual);
    }

    /**
     * A trailing {@code Z} after the milliseconds is not consumed by the custom
     * {@code yyyy-MM-dd'T'HH:mm:ss.SSS} pattern, so parsing falls back to {@code StdDateFormat} and the
     * {@code Z} (UTC) IS honored. The server timezone is forced to GMT+05:00 so a honored vs. dropped
     * {@code Z} yields different instants.
     */
    @Test
    public void testParse_zuluHonoredWhenMillisPresent() throws ParseException {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("GMT+05:00"));
            ExtendedStdDateFormat f = new ExtendedStdDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

            Date zoned = f.parse("2026-06-03T10:00:00.000Z");

            // 10:00 UTC == 15:00 in GMT+05:00.
            assertEquals(createDate(2026, 6, 3, 15, 0, 0), zoned);
            // The zone is no longer silently dropped, so it differs from the zone-less local reading.
            assertNotEquals(f.parse("2026-06-03T10:00:00.000"), zoned);
        } finally {
            TimeZone.setDefault(original);
        }
    }

    /**
     * Same as {@link #testParse_zuluHonoredWhenMillisPresent()} but with an explicit colon offset: the
     * {@code +09:00} after the milliseconds is honored.
     */
    @Test
    public void testParse_offsetHonoredWhenMillisPresent() throws ParseException {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("GMT+05:00"));
            ExtendedStdDateFormat f = new ExtendedStdDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

            Date zoned = f.parse("2026-06-03T10:00:00.000+09:00");

            // 10:00+09:00 == 01:00 UTC == 06:00 in GMT+05:00.
            assertEquals(createDate(2026, 6, 3, 6, 0, 0), zoned);
            assertNotEquals(f.parse("2026-06-03T10:00:00.000"), zoned);
        } finally {
            TimeZone.setDefault(original);
        }
    }

    /**
     * Numeric offset without a colon (e.g. {@code +0300}) after the milliseconds is honored too. This is the
     * shape sent by real clients (see the {@code EPBDS-7947} integration test).
     */
    @Test
    public void testParse_offsetWithoutColonHonoredWhenMillisPresent() throws ParseException {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("GMT+05:00"));
            ExtendedStdDateFormat f = new ExtendedStdDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

            Date zoned = f.parse("2026-06-03T10:00:00.000+0300");

            // 10:00+03:00 == 07:00 UTC == 12:00 in GMT+05:00.
            assertEquals(createDate(2026, 6, 3, 12, 0, 0), zoned);
        } finally {
            TimeZone.setDefault(original);
        }
    }

    /**
     * Without milliseconds the custom pattern does not match either, parsing falls back to
     * {@code StdDateFormat}, and the {@code Z} is honored.
     */
    @Test
    public void testParse_zuluHonoredWhenNoMillis() throws ParseException {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("GMT+05:00"));
            ExtendedStdDateFormat f = new ExtendedStdDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

            Date zoned = f.parse("2026-06-03T10:00:00Z");

            // 10:00 UTC == 15:00 in GMT+05:00.
            assertEquals(createDate(2026, 6, 3, 15, 0, 0), zoned);
        } finally {
            TimeZone.setDefault(original);
        }
    }

    /**
     * The zone-less value (no {@code Z}, no offset) is still read as local (server) time — the fix does not
     * change the no-zone behavior.
     */
    @Test
    public void testParse_noZoneStillLocal() throws ParseException {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("GMT+05:00"));
            ExtendedStdDateFormat f = new ExtendedStdDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS");

            assertEquals(createDate(2026, 6, 3, 10, 0, 0), f.parse("2026-06-03T10:00:00.000"));
        } finally {
            TimeZone.setDefault(original);
        }
    }

    /**
     * The {@code with*} methods of {@code StdDateFormat} return a plain {@code StdDateFormat}, which would
     * drop the custom pattern. Jackson calls them for {@code @JsonFormat(timezone/locale/lenient = ...)}
     * fields, so the overrides must keep the {@link ExtendedStdDateFormat} type and the configured pattern.
     */
    @Test
    public void testWithMethodsKeepExtendedTypeAndPattern() throws ParseException {
        ExtendedStdDateFormat base = new ExtendedStdDateFormat("dd/MM/yyyy HH:mm:ss");

        ExtendedStdDateFormat tz = base.withTimeZone(TimeZone.getTimeZone("GMT+05:00"));
        ExtendedStdDateFormat loc = base.withLocale(Locale.US);
        ExtendedStdDateFormat lenient = base.withLenient(Boolean.FALSE);
        ExtendedStdDateFormat colon = base.withColonInTimeZone(true);

        assertInstanceOf(ExtendedStdDateFormat.class, tz);
        assertInstanceOf(ExtendedStdDateFormat.class, loc);
        assertInstanceOf(ExtendedStdDateFormat.class, lenient);
        assertInstanceOf(ExtendedStdDateFormat.class, colon);

        // The custom (non-ISO) pattern still parses — a plain StdDateFormat copy could not.
        Date expected = createDate(2019, 12, 29, 4, 4, 4);
        assertEquals(expected, tz.withTimeZone(TimeZone.getDefault()).parse("29/12/2019 04:04:04"));
        assertEquals(expected, loc.parse("29/12/2019 04:04:04"));
        assertEquals(expected, lenient.parse("29/12/2019 04:04:04"));
        assertEquals(expected, colon.parse("29/12/2019 04:04:04"));
    }

    /**
     * {@code withTimeZone} keeps both the custom pattern and the zone-honoring parse fix: a zoned value is
     * still shifted to the bound time zone after the copy.
     */
    @Test
    public void testWithTimeZoneKeepsZoneHandling() throws ParseException {
        TimeZone original = TimeZone.getDefault();
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("GMT+05:00"));
            ExtendedStdDateFormat f =
                    new ExtendedStdDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").withTimeZone(TimeZone.getTimeZone("GMT+05:00"));

            // 10:00 UTC == 15:00 in GMT+05:00 — zone honored, not dropped.
            assertEquals(createDate(2026, 6, 3, 15, 0, 0), f.parse("2026-06-03T10:00:00.000Z"));
        } finally {
            TimeZone.setDefault(original);
        }
    }

    /**
     * The {@code with*} overrides return {@code this} when the requested value is already in effect, so they
     * do not allocate a redundant instance — mirroring {@code StdDateFormat}.
     */
    @Test
    public void testWithMethodsReturnSameInstanceWhenUnchanged() {
        ExtendedStdDateFormat tz =
                new ExtendedStdDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").withTimeZone(TimeZone.getTimeZone("GMT+05:00"));
        assertSame(tz, tz.withTimeZone(TimeZone.getTimeZone("GMT+05:00")));

        ExtendedStdDateFormat loc = tz.withLocale(Locale.US);
        assertSame(loc, loc.withLocale(Locale.US));

        ExtendedStdDateFormat lenient = tz.withLenient(Boolean.FALSE);
        assertSame(lenient, lenient.withLenient(Boolean.FALSE));

        ExtendedStdDateFormat colon = tz.withColonInTimeZone(tz.isColonIncludedInTimeZone());
        assertSame(colon, colon.withColonInTimeZone(colon.isColonIncludedInTimeZone()));
    }

    @Test
    public void testParseException() throws ParseException {
        assertThrows(ParseException.class, () -> {
            df.parse("asasas");
            fail("Oooops...");
        });
    }

    private static Date createDate(int year, int month, int dayOfMonth, int hour, int minute, int seconds) {
        Calendar cal = new GregorianCalendar();
        cal.set(year, month - 1, dayOfMonth, hour, minute, seconds);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

}
