package org.openl.rules.convertor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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

    // Overflows on both sides and a non-integer value
    @ParameterizedTest
    @ValueSource(strings = {"128", "-129", "1.3"})
    void testConvertInvalid(String value) {
        var converter = new String2ByteConvertor();
        assertThrows(NumberFormatException.class, () -> converter.parse(value, null));
    }

}
