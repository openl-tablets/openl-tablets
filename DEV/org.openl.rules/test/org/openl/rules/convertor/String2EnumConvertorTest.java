package org.openl.rules.convertor;

import static org.junit.Assert.*;

import org.junit.Test;

public class String2EnumConvertorTest {

    @Test
    public void testParse() {
        String2EnumConvertor<EnumVal> converter = new String2EnumConvertor<>(EnumVal.class);
        Enum<?> result = converter.parse("Val3", null);
        assertEquals(EnumVal.Val3, result);
    }

    @Test
    public void testParseCaseInsensetive() {
        String2EnumConvertor<EnumVal> converter = new String2EnumConvertor<>(EnumVal.class);
        Enum<?> result = converter.parse("vAl3", null);
        assertEquals(EnumVal.Val3, result);
    }

    @Test
    public void testParseOtherEnum() {
        String2EnumConvertor<EnumRes> converter = new String2EnumConvertor<>(EnumRes.class);
        Enum<?> result = converter.parse("Val3", null);
        assertNotEquals(EnumVal.Val3, result);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseNotPresent() {
        String2EnumConvertor<EnumRes> converter = new String2EnumConvertor<>(EnumRes.class);
        converter.parse("Val4", null);
    }

    @Test
    public void testParseNull() {
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
