package org.openl.rules.convertor;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class String2BooleanConvertorTest {

    @Test
    public void testParseTrue() {
        String2BooleanConvertor converter = new String2BooleanConvertor();
        Boolean result = converter.parse("True", null);
        assertEquals(Boolean.TRUE, result);
    }

    @Test
    public void testParseFalse() {
        String2BooleanConvertor converter = new String2BooleanConvertor();
        Boolean result = converter.parse("false", null);
        assertEquals(Boolean.FALSE, result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseEmpty() {
        String2BooleanConvertor converter = new String2BooleanConvertor();
        converter.parse("", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseWrongValue() {
        String2BooleanConvertor converter = new String2BooleanConvertor();
        converter.parse("1", null);
    }

    @Test
    public void testParseNull() {
        String2BooleanConvertor converter = new String2BooleanConvertor();
        assertNull(converter.parse(null, null));
    }

}
