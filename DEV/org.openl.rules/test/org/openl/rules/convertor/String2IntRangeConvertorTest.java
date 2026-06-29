package org.openl.rules.convertor;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class String2IntRangeConvertorTest {

    @Test
    void testParseNull() {
        String2IntRangeConvertor converter = new String2IntRangeConvertor();
        assertNull(converter.parse(null, null));
    }

}
