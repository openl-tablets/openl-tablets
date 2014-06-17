package org.openl.rules.convertor;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class String2CharConvertorTest {

    @Test
    public void testParse() {
        String2CharConvertor converter = new String2CharConvertor();
        Character result = converter.parse("X", null, null);
        assertEquals(new Character('X'), result);
    }

    @Test
    public void testFormat() {
        String2CharConvertor converter = new String2CharConvertor();
        String result = converter.format('@', null);
        assertEquals("@", result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseEmpty() {
        String2BooleanConvertor converter = new String2BooleanConvertor();
        converter.parse("", null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseWrongValue() {
        String2BooleanConvertor converter = new String2BooleanConvertor();
        converter.parse("12", null, null);
    }

    @Test
    public void testParseNull() {
        String2CharConvertor converter = new String2CharConvertor();
        assertNull(converter.parse(null, null, null));
    }

    @Test
    public void testFormatNull() {
        String2CharConvertor converter = new String2CharConvertor();
        assertNull(converter.format(null, null));
    }
}
