package org.openl.rules.convertor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

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

    @Test
    public void testParseEmpty() {
        assertThrows(IllegalArgumentException.class, () -> {
            String2BooleanConvertor converter = new String2BooleanConvertor();
            converter.parse("", null);
        });
    }

    @Test
    public void testParseWrongValue() {
        assertThrows(IllegalArgumentException.class, () -> {
            String2BooleanConvertor converter = new String2BooleanConvertor();
            converter.parse("1", null);
        });
    }

    @Test
    public void testParseNull() {
        String2BooleanConvertor converter = new String2BooleanConvertor();
        assertNull(converter.parse(null, null));
    }

}
