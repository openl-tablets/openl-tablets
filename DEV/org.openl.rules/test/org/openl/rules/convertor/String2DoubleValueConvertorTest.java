package org.openl.rules.convertor;

import org.junit.Test;

import static org.junit.Assert.assertNull;

public class String2DoubleValueConvertorTest {

    @Test
    public void testParseNull() {
        String2DoubleValueConvertor converter = new String2DoubleValueConvertor();
        assertNull(converter.parse(null, null));
    }

}
