package org.openl.rules.convertor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class String2ConstructorConvertorTest {

    @Test
    public void testParse() {
        String2ConstructorConvertor<Integer> converter = new String2ConstructorConvertor<>(Integer.class);
        Integer result = converter.parse("123", null);
        assertEquals((Integer) 123, result);
    }

    @Test
    public void testParseNull() {
        String2ConstructorConvertor<?> converter = new String2ConstructorConvertor<>(Integer.class);
        assertNull(converter.parse(null, null));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testParseNoConstructor() {
        new String2ConstructorConvertor<>(Object.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testFormatNoConstructor() {
        new String2ConstructorConvertor<>(Object.class);
    }
}
