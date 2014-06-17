package org.openl.rules.convertor;

import org.junit.Test;

import static org.junit.Assert.assertNull;

public class String2ArrayConvertorTest {

    @Test
    public void testParseNull() {
        String2ArrayConvertor converter = new String2ArrayConvertor(null);
        assertNull(converter.parse(null, null, null));
    }

    @Test
    public void testFormatNull() {
        String2ArrayConvertor converter = new String2ArrayConvertor(null);
        assertNull(converter.format(null, null));
    }
}
