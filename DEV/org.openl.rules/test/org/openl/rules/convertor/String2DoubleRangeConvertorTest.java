package org.openl.rules.convertor;

import static org.junit.Assert.assertNull;

import org.junit.Test;

public class String2DoubleRangeConvertorTest {

    @Test
    public void testParseNull() {
        String2DoubleRangeConvertor converter = new String2DoubleRangeConvertor();
        assertNull(converter.parse(null, null));
    }

}
