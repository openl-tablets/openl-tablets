package org.openl.rules.convertor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class String2EnumConvertorTest {

    @Test
    void testParse() {
        String2EnumConvertor<EnumVal> converter = new String2EnumConvertor<>(EnumVal.class);
        Enum<?> result = converter.parse("Val3", null);
        assertEquals(EnumVal.Val3, result);
    }

    @Test
    void testParseCaseInsensetive() {
        String2EnumConvertor<EnumVal> converter = new String2EnumConvertor<>(EnumVal.class);
        Enum<?> result = converter.parse("vAl3", null);
        assertEquals(EnumVal.Val3, result);
    }

    @Test
    void testParseOtherEnum() {
        String2EnumConvertor<EnumRes> converter = new String2EnumConvertor<>(EnumRes.class);
        Enum<?> result = converter.parse("Val3", null);
        assertNotEquals(EnumVal.Val3, result);
    }

    @Test
    void testParseNotPresent() {
        assertThrows(IllegalArgumentException.class, () -> {
            String2EnumConvertor<EnumRes> converter = new String2EnumConvertor<>(EnumRes.class);
            converter.parse("Val4", null);
        });
    }

    @Test
    void testParseNull() {
        String2EnumConvertor<EnumRes> converter = new String2EnumConvertor<>(null);
        assertNull(converter.parse(null, null));
    }

    private enum EnumVal {
        VAL1,
        val2,
        Val3
    }

    private enum EnumRes {
        RES1,
        res2,
        Val3
    }
}
