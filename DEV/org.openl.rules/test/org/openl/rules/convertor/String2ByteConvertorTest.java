package org.openl.rules.convertor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class String2ByteConvertorTest {

    @Test
    void testConvertPositive() {
        String2ByteConvertor converter = new String2ByteConvertor();
        Number result = converter.parse("127", null);
        assertEquals(Byte.MAX_VALUE, result);
    }

    @Test
    void testConvertNegative() {
        String2ByteConvertor converter = new String2ByteConvertor();
        Number result = converter.parse("-128", null);
        assertEquals(Byte.MIN_VALUE, result);
    }

    @Test
    void testConvertPositiveOverflow() {
        assertThrows(NumberFormatException.class, () -> {
            String2ByteConvertor converter = new String2ByteConvertor();
            converter.parse("128", null);
        });
    }

    @Test
    void testConvertNegativeOverflow() {
        assertThrows(NumberFormatException.class, () -> {
            String2ByteConvertor converter = new String2ByteConvertor();
            converter.parse("-129", null);
        });
    }

    @Test
    void testConvertNonInteger() {
        assertThrows(NumberFormatException.class, () -> {
            String2ByteConvertor converter = new String2ByteConvertor();
            converter.parse("1.3", null);
        });
    }

}
