package org.openl.rules.convertor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class String2DateConvertorTest {

    private static final Locale DEFAULT_LOCALE = Locale.getDefault();
    private static final TimeZone DEFAULT_TIMEZONE = TimeZone.getDefault();

    private Locale defaultLocale;

    @BeforeEach
    public void setupLocale() {
        defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
    }

    @AfterEach
    public void restoreLocale() {
        Locale.setDefault(defaultLocale);
    }

    @Test
    public void testParse() {
        String2DateConvertor converter = new String2DateConvertor();
        Date result = converter.parse("06/17/2014", null);
        assertEquals(new Date(114, 5, 17), result);
    }

    @Test
    public void testParseByPattern() {
        String2DateConvertor converter = new String2DateConvertor();
        Date result = converter.parse("17-06-2014", "dd-MM-yyyy");
        assertEquals(new Date(114, 5, 17), result);
    }

    @Test
    public void testParseEmpty() {
        assertThrows(IllegalArgumentException.class, () -> {
            String2DateConvertor converter = new String2DateConvertor();
            converter.parse("", null);
        });
    }

    @Test
    public void testParseWrongValue() {
        assertThrows(IllegalArgumentException.class, () -> {
            String2DateConvertor converter = new String2DateConvertor();
            converter.parse("Kin-Dza-Dza", null);
        });
    }

    @Test
    public void testParseExtraSymbol() {
        assertThrows(IllegalArgumentException.class, () -> {
            String2DateConvertor converter = new String2DateConvertor();
            converter.parse("2021-01-01T", null);
        });
    }

    @Test
    public void testParseMissprint() {
        assertThrows(IllegalArgumentException.class, () -> {
            String2DateConvertor converter = new String2DateConvertor();
            converter.parse("10/13/20 17", null);
        });
    }

    @Test
    public void testParseNull() {
        String2DateConvertor converter = new String2DateConvertor();
        assertNull(converter.parse(null, null));
    }

    @Test
    public void testParseUSDateTime() {
        String2DateConvertor converter = new String2DateConvertor();
        Date result = converter.parse("04/01/2021 12:00 AM", null);
        assertEquals(createDate(2021, 4, 1, 0, 0, 0, 0), result);

        result = converter.parse("8/1/2013 11:59 PM", null);
        assertEquals(createDate(2013, 8, 1, 23, 59, 0, 0), result);

        result = converter.parse("04/01/2021 10:00", null);
        assertEquals(createDate(2021, 4, 1, 10, 0, 0, 0), result);

        result = converter.parse("5/14/1789 3:30:10", null);
        assertEquals(createDate(1789, 5, 14, 3, 30, 10, 0), result);

        result = converter.parse("04/01/2021", null);
        assertEquals(createDate(2021, 4, 1, 0, 0, 0, 0), result);

        result = converter.parse("7/12/80", null);
        assertEquals(createDate(1980, 7, 12, 0, 0, 0, 0), result);

        result = converter.parse("04/01/2021 20:00", null);
        assertEquals(createDate(2021, 4, 1, 20, 0, 0, 0), result);

        result = converter.parse("04/01/2021 20:00:24", null);
        assertEquals(createDate(2021, 4, 1, 20, 0, 24, 0), result);
    }

    @Test
    public void testParseISO8601() {
        String2DateConvertor converter = new String2DateConvertor();

        Date result = converter.parse("2021-01-01T01:01", null);
        assertEquals(createDate(2021, 1, 1, 1, 1, 0, 0), result);

        result = converter.parse("2021-01-01T01:01:01.000", null);
        assertEquals(createDate(2021, 1, 1, 1, 1, 1, 0), result);
    }

    @Test
    public void testParseISO8601WithTimeZone() {
        try {
            // set +2 as default
            setUpTimeZone(TimeZone.getTimeZone("Europe/Helsinki"));
            String2DateConvertor converter = new String2DateConvertor();

            Date result = converter.parse("2021-01-01T01:01", null);
            assertEquals(createDate(2021, 1, 1, 1, 1, 0, 0), result);

            result = converter.parse("2021-01-01T01:01:01.000", null);
            assertEquals(createDate(2021, 1, 1, 1, 1, 1, 0), result);

            result = converter.parse("7777777-07-07T22:7:7.000", null);
            assertEquals(createDate(7777777, 7, 7, 22, 7, 7, 0), result);

            result = converter.parse("2021-01-01T01:01:01.000Z", null);
            assertEquals(createDate(2021, 1, 1, 3, 1, 1, 0), result);

            result = converter.parse("2021-01-01T01:01:01.000+00", null);
            assertEquals(createDate(2021, 1, 1, 3, 1, 1, 0), result);

            result = converter.parse("2021-02-03T14:15:16.123+05", null);
            assertEquals(createDate(2021, 2, 3, 11, 15, 16, 123), result);

            result = converter.parse("2021-02-03T14:15:16.123+05:30", null);
            assertEquals(createDate(2021, 2, 3, 10, 45, 16, 123), result);

            result = converter.parse("2021-02-03T14:15:16.1239+00:00", null);
            assertEquals(createDate(2021, 2, 3, 16, 15, 16, 123), result);

            result = converter.parse("2021-01-01T01:01:01.000+02", null);
            assertEquals(createDate(2021, 1, 1, 1, 1, 1, 0), result);
            // Day saving time
            result = converter.parse("2021-06-01T01:01:01.000Z", null);
            assertEquals(createDate(2021, 6, 1, 4, 1, 1, 0), result);
        } finally {
            tearDownTimeZone();
        }
    }

    public static void setUpTimeZone(TimeZone defaultTimeZone) {
        Locale.setDefault(Locale.US);
        TimeZone.setDefault(defaultTimeZone);
    }

    public static void tearDownTimeZone() {
        Locale.setDefault(DEFAULT_LOCALE);
        TimeZone.setDefault(DEFAULT_TIMEZONE);
    }

    private static Date createDate(int year, int month, int dayOfMonth, int hour, int minute, int seconds, int mills) {
        Calendar cal = new GregorianCalendar();
        cal.set(year, month - 1, dayOfMonth, hour, minute, seconds);
        cal.set(Calendar.MILLISECOND, mills);
        return cal.getTime();
    }

}
