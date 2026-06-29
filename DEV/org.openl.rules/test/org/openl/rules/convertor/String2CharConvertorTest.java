package org.openl.rules.convertor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class String2CharConvertorTest {

    @Test
    void testParse() {
        String2CharConvertor converter = new String2CharConvertor();
        Character result = converter.parse("X", null);
        assertEquals(Character.valueOf('X'), result);
    }

    @Test
    void testParseEmpty() {
        assertThrows(IllegalArgumentException.class, () -> {
            String2BooleanConvertor converter = new String2BooleanConvertor();
            converter.parse("", null);
        });
    }

    @Test
    void testParseWrongValue() {
        assertThrows(IllegalArgumentException.class, () -> {
            String2BooleanConvertor converter = new String2BooleanConvertor();
            converter.parse("12", null);
        });
    }

    @Test
    void testParseNull() {
        String2CharConvertor converter = new String2CharConvertor();
        assertNull(converter.parse(null, null));
    }

}
