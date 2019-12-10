package org.openl.rules.ruleservice.databinding.util;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class ExtendedStdDateFormatTest {

    private ExtendedStdDateFormat df;

    @Before
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

    @Test(expected = ParseException.class)
    public void testParseException() throws ParseException {
        df.parse("asasas");
        fail("Oooops...");
    }

    private static Date createDate(int year, int month, int dayOfMonth, int hour, int minute, int seconds) {
        Calendar cal = new GregorianCalendar();
        cal.set(year, month - 1, dayOfMonth, hour, minute, seconds);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

}
