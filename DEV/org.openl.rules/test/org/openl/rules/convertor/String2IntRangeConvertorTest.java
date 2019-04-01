package org.openl.rules.convertor;

import static org.junit.Assert.assertNull;

import org.junit.Test;

public class String2IntRangeConvertorTest {

    @Test
    public void testParseNull() {
        String2IntRangeConvertor converter = new String2IntRangeConvertor();
        assertNull(converter.parse(null, null));
    }

}
