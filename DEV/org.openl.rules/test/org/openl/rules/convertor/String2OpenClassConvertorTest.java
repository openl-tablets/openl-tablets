package org.openl.rules.convertor;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class String2OpenClassConvertorTest {

    @Test
    void testParseNull() {
        var converter = new String2OpenClassConvertor();
        assertNull(converter.parse(null, null, null));
    }

}
