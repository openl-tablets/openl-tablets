package org.openl.rules.convertor;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class String2ClassConvertorTest {

    @Test
    public void testParseNull() {
        String2ClassConvertor converter = new String2ClassConvertor();
        assertNull(converter.parse(null, null, null));
    }

}
