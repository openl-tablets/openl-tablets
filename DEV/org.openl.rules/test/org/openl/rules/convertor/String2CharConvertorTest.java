package org.openl.rules.convertor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class String2CharConvertorTest {

    @Test
    public void testParse() {
        String2CharConvertor converter = new String2CharConvertor();
        Character result = converter.parse("X", null);
        assertEquals(new Character('X'), result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseEmpty() {
        String2BooleanConvertor converter = new String2BooleanConvertor();
        converter.parse("", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseWrongValue() {
        String2BooleanConvertor converter = new String2BooleanConvertor();
        converter.parse("12", null);
    }

    @Test
    public void testParseNull() {
        String2CharConvertor converter = new String2CharConvertor();
        assertNull(converter.parse(null, null));
    }

}
