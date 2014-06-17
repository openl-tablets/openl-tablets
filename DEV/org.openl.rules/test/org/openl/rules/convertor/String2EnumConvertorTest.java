package org.openl.rules.convertor;

import org.junit.Test;

import static org.junit.Assert.assertNull;

public class String2EnumConvertorTest {

    @Test
    public void testParseNull() {
        String2EnumConvertor converter = new String2EnumConvertor(null);
        assertNull(converter.parse(null, null, null));
    }

    @Test
    public void testFormatNull() {
        String2EnumConvertor converter = new String2EnumConvertor(null);
        assertNull(converter.format(null, null));
    }
}
