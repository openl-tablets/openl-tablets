package org.openl.rules.convertor;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class String2BooleanConvertorTest {

    @Test
    public void testParseTrue() {
        String2BooleanConvertor converter = new String2BooleanConvertor();
        Boolean result = converter.parse("True", null, null);
        assertEquals(Boolean.TRUE, result);
    }

    @Test
    public void testParseFalse() {
        String2BooleanConvertor converter = new String2BooleanConvertor();
        Boolean result = converter.parse("false", null, null);
        assertEquals(Boolean.FALSE, result);
    }

    @Test
    public void testFormatTrue() {
        String2BooleanConvertor converter = new String2BooleanConvertor();
        String result = converter.format(true, null);
        assertEquals("true", result);
    }

    @Test
    public void testFormatFalse() {
        String2BooleanConvertor converter = new String2BooleanConvertor();
        String result = converter.format(false, null);
        assertEquals("false", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseEmpty() {
        String2BooleanConvertor converter = new String2BooleanConvertor();
        converter.parse("", null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseWrongValue() {
        String2BooleanConvertor converter = new String2BooleanConvertor();
        converter.parse("1", null, null);
    }

    @Test
    public void testParseNull() {
        String2BooleanConvertor converter = new String2BooleanConvertor();
        assertNull(converter.parse(null, null, null));
    }

    @Test
    public void testFormatNull() {
        String2BooleanConvertor converter = new String2BooleanConvertor();
        assertNull(converter.format(null, null));
    }
}
