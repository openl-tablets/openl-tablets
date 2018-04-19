package org.openl.rules.convertor;

import org.junit.Test;

import static org.junit.Assert.assertNull;

public class String2IntRangeConvertorTest {

    @Test
    public void testParseNull() {
        String2IntRangeConvertor converter = new String2IntRangeConvertor();
        assertNull(converter.parse(null, null));
    }

}
