package org.openl.rules.convertor;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class String2ClassConvertorTest {

    @Test
    public void testParseNull() {
        String2ClassConvertor converter = new String2ClassConvertor();
        assertNull(converter.parse(null, null, null));
    }

}
