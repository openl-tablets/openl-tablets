package org.openl.rules.convertor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class String2DateConvertorTest {

    private static final Locale DEFAULT_LOCALE = Locale.getDefault();
    private static final TimeZone DEFAULT_TIMEZONE = TimeZone.getDefault();

    private Locale defaultLocale;

    @Before
    public void setupLocale() {
        defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
    }

    @After
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

    @Test(expected = IllegalArgumentException.class)
    public void testParseEmpty() {
        String2DateConvertor converter = new String2DateConvertor();
        converter.parse("", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseWrongValue() {
        String2DateConvertor converter = new String2DateConvertor();
        converter.parse("Kin-Dza-Dza", null);
    }

    @Test
    public void testParseNull() {
        String2DateConvertor converter = new String2DateConvertor();
        assertNull(converter.parse(null, null));
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

            result = converter.parse("2021-01-01T01:01:01.000Z", null);
            assertEquals(createDate(2021, 1, 1, 3, 1, 1, 0), result);
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
