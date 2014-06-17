package org.openl.rules.convertor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class String2CalendarConvertorTest {

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
        Calendar time = Calendar.getInstance();
        time.setTime(new Date(114, 5, 17));
        String2CalendarConvertor converter = new String2CalendarConvertor();
        Calendar result = converter.parse("06/17/2014", null, null);
        assertEquals(time, result);
    }

    @Test
    public void testParseByPattern() {
        Calendar time = Calendar.getInstance();
        time.setTime(new Date(114, 5, 17));
        String2CalendarConvertor converter = new String2CalendarConvertor();
        Calendar result = converter.parse("17-06-2014", "dd-MM-yyyy", null);
        assertEquals(time, result);
    }

    @Test
    public void testFormat() {
        Calendar time = Calendar.getInstance();
        time.setTime(new Date(1402952400000L));
        String2CalendarConvertor converter = new String2CalendarConvertor();
        String result = converter.format(time, null);
        assertEquals("6/17/14", result);
    }

    @Test
    public void testFormatByPattern() {
        Calendar time = Calendar.getInstance();
        time.setTime(new Date(1402952400000L));
        String2CalendarConvertor converter = new String2CalendarConvertor();
        String result = converter.format(time, "MM/dd/yyyy");
        assertEquals("06/17/2014", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseEmpty() {
        String2CalendarConvertor converter = new String2CalendarConvertor();
        converter.parse("", null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseWrongValue() {
        String2CalendarConvertor converter = new String2CalendarConvertor();
        converter.parse("Kin-Dza-Dza", null, null);
    }

    @Test
    public void testParseNull() {
        String2CalendarConvertor converter = new String2CalendarConvertor();
        assertNull(converter.parse(null, null, null));
    }

    @Test
    public void testFormatNull() {
        String2CalendarConvertor converter = new String2CalendarConvertor();
        assertNull(converter.format(null, null));
    }
}
