package org.openl.rules.convertor;

import org.apache.commons.lang.exception.NestableRuntimeException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class String2DoubleConvertorTest {

    private Locale defaultLocale;

    @Before
    public void setupLocale() {
        defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.GERMAN);
    }

    @After
    public void restoreLocale() {
        Locale.setDefault(defaultLocale);
    }

    @Test
    public void testParse() {
        IString2DataConvertor conv = new String2DoubleConvertor();
        Object res = conv.parse("3.1415", null, null);
        assertEquals(3.1415d, res);
    }

    @Test(expected = NumberFormatException.class)
    public void testParseNonNumber() {
        IString2DataConvertor conv = new String2DoubleConvertor();
        conv.parse("3.1415d", null, null);
    }

    @Test(expected = NumberFormatException.class)
    public void testParseEmpty() {
        IString2DataConvertor conv = new String2DoubleConvertor();
        conv.parse("", null, null);
    }

    @Test
    public void testParsePercents() {
        IString2DataConvertor conv = new String2DoubleConvertor();
        Object res = conv.parse("17.5%", null, null);
        assertEquals(0.175d, res);
    }

    @Test
    public void testFormat() {
        IString2DataConvertor conv = new String2DoubleConvertor();
        String res = conv.format(3.1415d, null);
        assertEquals("3.14", res);
    }
    
    @Test
    public void testParseNull() {
        IString2DataConvertor conv = new String2DoubleConvertor();
        assertNull(conv.parse(null, null, null));
    }
    
    @Test
    public void testFormatNull() {
        IString2DataConvertor conv = new String2DoubleConvertor();
        assertNull(conv.format(null, null));
    }

}
