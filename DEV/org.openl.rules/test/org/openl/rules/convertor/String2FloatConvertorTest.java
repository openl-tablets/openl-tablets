package org.openl.rules.convertor;

import org.apache.commons.lang.exception.NestableRuntimeException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class String2FloatConvertorTest {

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
        IString2DataConvertor conv = new String2FloatConvertor();
        Object res = conv.parse("3.1415", null, null);
        assertEquals(3.1415f, res);
    }

    @Test(expected = NestableRuntimeException.class)
    public void testParseNonNumber() {
        IString2DataConvertor conv = new String2FloatConvertor();
        conv.parse("3.1415f", null, null);
    }

    @Test(expected = NestableRuntimeException.class)
    public void testParseEmpty() {
        IString2DataConvertor conv = new String2FloatConvertor();
        conv.parse("", null, null);
    }

    @Test
    public void testParsePercents() {
        IString2DataConvertor conv = new String2FloatConvertor();
        Object res = conv.parse("17.3%", null, null);
        assertEquals(0.173f, res);
    }

    @Test
    public void testFormat() {
        IString2DataConvertor conv = new String2FloatConvertor();
        String res = conv.format(3.1415f, null);
        assertEquals("3.14", res);
    }
    
    @Test
    public void testParseNull() {
        IString2DataConvertor conv = new String2FloatConvertor();
        assertNull(conv.parse(null, null, null));
    }
    
    @Test
    public void testFormatNull() {
        IString2DataConvertor conv = new String2FloatConvertor();
        assertNull(conv.format(null, null));
    }

}
