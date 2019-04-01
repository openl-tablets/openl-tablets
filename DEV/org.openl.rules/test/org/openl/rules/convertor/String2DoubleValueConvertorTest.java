package org.openl.rules.convertor;

import static org.junit.Assert.assertNull;

import org.junit.Test;

public class String2DoubleValueConvertorTest {

    @Test
    public void testParseNull() {
        String2DoubleValueConvertor converter = new String2DoubleValueConvertor();
        assertNull(converter.parse(null, null));
    }

}
