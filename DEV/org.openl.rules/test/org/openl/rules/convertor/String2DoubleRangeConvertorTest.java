package org.openl.rules.convertor;

import org.junit.Test;

import static org.junit.Assert.assertNull;

public class String2DoubleRangeConvertorTest {

    @Test
    public void testParseNull() {
        String2DoubleRangeConvertor converter = new String2DoubleRangeConvertor();
        assertNull(converter.parse(null, null));
    }

}
