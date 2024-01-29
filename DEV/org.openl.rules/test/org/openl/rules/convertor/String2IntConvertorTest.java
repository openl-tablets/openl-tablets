package org.openl.rules.convertor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class String2IntConvertorTest {

    @Test
    public void testConvertPositive() {
        String2IntConvertor converter = new String2IntConvertor();
        Number result = converter.parse("2147483647", null);
        assertEquals(Integer.MAX_VALUE, result);
    }

    @Test
    public void testConvertNegative() {
        String2IntConvertor converter = new String2IntConvertor();
        Number result = converter.parse("-2147483648", null);
        assertEquals(Integer.MIN_VALUE, result);
    }

    @Test
    public void testConvertPositiveOverflow() {
        assertThrows(NumberFormatException.class, () -> {
            String2IntConvertor converter = new String2IntConvertor();
            converter.parse("2147483648", null);
        });
    }

    @Test
    public void testConvertNegativeOverflow() {
        assertThrows(NumberFormatException.class, () -> {
            String2IntConvertor converter = new String2IntConvertor();
            converter.parse("-2147483649", null);
        });
    }

    @Test
    public void testConvertNonInteger() {
        assertThrows(NumberFormatException.class, () -> {
            String2IntConvertor converter = new String2IntConvertor();
            converter.parse("1.3", null);
        });
    }

}
