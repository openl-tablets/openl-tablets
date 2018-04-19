package org.openl.rules.convertor;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class String2DateConvertorTest {

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

}
