package org.openl.rules.convertor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class String2BooleanConvertorTest {

    @Test
    void testParseTrue() {
        var converter = new String2BooleanConvertor();
        var result = converter.parse("True", null);
        assertEquals(Boolean.TRUE, result);
    }

    @Test
    void testParseFalse() {
        var converter = new String2BooleanConvertor();
        var result = converter.parse("false", null);
        assertEquals(Boolean.FALSE, result);
    }

    @Test
    void testParseEmpty() {
        assertThrows(IllegalArgumentException.class, () -> {
            var converter = new String2BooleanConvertor();
            converter.parse("", null);
        });
    }

    @Test
    void testParseWrongValue() {
        assertThrows(IllegalArgumentException.class, () -> {
            var converter = new String2BooleanConvertor();
            converter.parse("1", null);
        });
    }

    @Test
    void testParseNull() {
        var converter = new String2BooleanConvertor();
        assertNull(converter.parse(null, null));
    }

}
