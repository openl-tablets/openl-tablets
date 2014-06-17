package org.openl.rules.convertor;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class String2ClassConvertorTest {

    @Test
    public void testFormatPrimitive() {
        String2ClassConvertor converter = new String2ClassConvertor();
        String result = converter.format(double.class, null);
        assertEquals("double", result);
    }

    @Test
    public void testFormatObject() {
        String2ClassConvertor converter = new String2ClassConvertor();
        String result = converter.format(Double.class, null);
        assertEquals("class java.lang.Double", result);
    }

    @Test
    public void testFormatArrayOfPrimitives() {
        String2ClassConvertor converter = new String2ClassConvertor();
        String result = converter.format(double[].class, null);
        assertEquals("class [D", result);
    }


    @Test
    public void testFormatArrayOfObjects() {
        String2ClassConvertor converter = new String2ClassConvertor();
        String result = converter.format(Double[].class, null);
        assertEquals("class [Ljava.lang.Double;", result);
    }

    @Test
    public void testParseNull() {
        String2ClassConvertor converter = new String2ClassConvertor();
        assertNull(converter.parse(null, null, null));
    }

    @Test
    public void testFormatNull() {
        String2ClassConvertor converter = new String2ClassConvertor();
        assertNull(converter.format(null, null));
    }
}
