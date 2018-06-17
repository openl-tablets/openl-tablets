package org.openl.rules.convertor;

import org.junit.Test;

import static org.junit.Assert.assertNull;

public class String2OpenClassConvertorTest {

    @Test
    public void testParseNull() {
        String2OpenClassConvertor converter = new String2OpenClassConvertor();
        assertNull(converter.parse(null, null, null));
    }

}
