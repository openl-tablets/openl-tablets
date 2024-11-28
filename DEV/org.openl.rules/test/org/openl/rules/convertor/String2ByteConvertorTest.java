package org.openl.rules.convertor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class String2ByteConvertorTest {

    @Test
    public void testConvertPositive() {
        String2ByteConvertor converter = new String2ByteConvertor();
        Number result = converter.parse("127", null);
        assertEquals(Byte.MAX_VALUE, result);
    }

    @Test
    public void testConvertNegative() {
        String2ByteConvertor converter = new String2ByteConvertor();
        Number result = converter.parse("-128", null);
        assertEquals(Byte.MIN_VALUE, result);
    }

    @Test
    public void testConvertPositiveOverflow() {
        assertThrows(NumberFormatException.class, () -> {
            String2ByteConvertor converter = new String2ByteConvertor();
            converter.parse("128", null);
        });
    }

    @Test
    public void testConvertNegativeOverflow() {
        assertThrows(NumberFormatException.class, () -> {
            String2ByteConvertor converter = new String2ByteConvertor();
            converter.parse("-129", null);
        });
    }

    @Test
    public void testConvertNonInteger() {
        assertThrows(NumberFormatException.class, () -> {
            String2ByteConvertor converter = new String2ByteConvertor();
            converter.parse("1.3", null);
        });
    }

}
