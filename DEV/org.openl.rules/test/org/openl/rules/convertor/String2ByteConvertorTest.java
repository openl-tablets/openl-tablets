package org.openl.rules.convertor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class String2ByteConvertorTest {

    @Test
    void testConvertPositive() {
        var converter = new String2ByteConvertor();
        var result = converter.parse("127", null);
        assertEquals(Byte.MAX_VALUE, result);
    }

    @Test
    void testConvertNegative() {
        var converter = new String2ByteConvertor();
        var result = converter.parse("-128", null);
        assertEquals(Byte.MIN_VALUE, result);
    }

    @Test
    void testConvertPositiveOverflow() {
        var converter = new String2ByteConvertor();
        assertThrows(NumberFormatException.class, () -> converter.parse("128", null));
    }

    @Test
    void testConvertNegativeOverflow() {
        var converter = new String2ByteConvertor();
        assertThrows(NumberFormatException.class, () -> converter.parse("-129", null));
    }

    @Test
    void testConvertNonInteger() {
        var converter = new String2ByteConvertor();
        assertThrows(NumberFormatException.class, () -> converter.parse("1.3", null));
    }

}
