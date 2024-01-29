package org.openl.rules.convertor;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class String2IntRangeConvertorTest {

    @Test
    public void testParseNull() {
        String2IntRangeConvertor converter = new String2IntRangeConvertor();
        assertNull(converter.parse(null, null));
    }

}
