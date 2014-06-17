package org.openl.rules.convertor;

import org.junit.Test;

import static org.junit.Assert.assertNull;

public class String2ConstructorConvertorTest {

    @Test
    public void testParseNull() {
        String2ConstructorConvertor converter = new String2ConstructorConvertor(null);
        assertNull(converter.parse(null, null, null));
    }

    @Test
    public void testFormatNull() {
        String2ConstructorConvertor converter = new String2ConstructorConvertor(null);
        assertNull(converter.format(null, null));
    }
}
