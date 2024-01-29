package org.openl.rules.convertor;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class String2OpenClassConvertorTest {

    @Test
    public void testParseNull() {
        String2OpenClassConvertor converter = new String2OpenClassConvertor();
        assertNull(converter.parse(null, null, null));
    }

}
