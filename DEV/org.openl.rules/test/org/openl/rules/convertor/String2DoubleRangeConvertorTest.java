package org.openl.rules.convertor;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class String2DoubleRangeConvertorTest {

    @Test
    void testParseNull() {
        String2DoubleRangeConvertor converter = new String2DoubleRangeConvertor();
        assertNull(converter.parse(null, null));
    }

}
