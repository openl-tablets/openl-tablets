package org.openl.rules.convertor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class String2CharConvertorTest {

    @Test
    void testParse() {
        var converter = new String2CharConvertor();
        var result = converter.parse("X", null);
        assertEquals(Character.valueOf('X'), result);
    }

    @Test
    void testParseEmpty() {
        var converter = new String2BooleanConvertor();
        assertThrows(IllegalArgumentException.class, () -> converter.parse("", null));
    }

    @Test
    void testParseWrongValue() {
        var converter = new String2BooleanConvertor();
        assertThrows(IllegalArgumentException.class, () -> converter.parse("12", null));
    }

    @Test
    void testParseNull() {
        var converter = new String2CharConvertor();
        assertNull(converter.parse(null, null));
    }

}
