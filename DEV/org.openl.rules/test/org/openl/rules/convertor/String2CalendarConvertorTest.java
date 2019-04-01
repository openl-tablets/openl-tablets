package org.openl.rules.convertor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

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
        Calendar result = converter.parse("06/17/2014", null);
        assertEquals(time, result);
    }

    @Test
    public void testParseByPattern() {
        Calendar time = Calendar.getInstance();
        time.setTime(new Date(114, 5, 17));
        String2CalendarConvertor converter = new String2CalendarConvertor();
        Calendar result = converter.parse("17-06-2014", "dd-MM-yyyy");
        assertEquals(time, result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseEmpty() {
        String2CalendarConvertor converter = new String2CalendarConvertor();
        converter.parse("", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseWrongValue() {
        String2CalendarConvertor converter = new String2CalendarConvertor();
        converter.parse("Kin-Dza-Dza", null);
    }

    @Test
    public void testParseNull() {
        String2CalendarConvertor converter = new String2CalendarConvertor();
        assertNull(converter.parse(null, null));
    }

}
