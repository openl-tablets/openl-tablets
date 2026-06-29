package org.openl.rules.convertor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class String2BooleanConvertorTest {

    @Test
    void testParseTrue() {
        String2BooleanConvertor converter = new String2BooleanConvertor();
        Boolean result = converter.parse("True", null);
        assertEquals(Boolean.TRUE, result);
    }

    @Test
    void testParseFalse() {
        String2BooleanConvertor converter = new String2BooleanConvertor();
        Boolean result = converter.parse("false", null);
        assertEquals(Boolean.FALSE, result);
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
            converter.parse("1", null);
        });
    }

    @Test
    void testParseNull() {
        String2BooleanConvertor converter = new String2BooleanConvertor();
        assertNull(converter.parse(null, null));
    }

}
