package org.openl.rules.convertor;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class String2ArrayConvertorTest {

    @Test
    void testParseEmpty() {
        String2ArrayConvertor<Integer, Integer[]> converter = new String2ArrayConvertor<>(Integer.class);
        Integer[] result = converter.parse("", null);
        assertArrayEquals(new Integer[]{}, result);
    }

    @Test
    void testParseSingleElement() {
        String2ArrayConvertor<Integer, Integer[]> converter = new String2ArrayConvertor<>(Integer.class);
        Integer[] result = converter.parse("123", null);
        assertArrayEquals(new Integer[]{123}, result);
    }

    @Test
    void testParseTwoElements() {
        String2ArrayConvertor<Integer, Integer[]> converter = new String2ArrayConvertor<>(Integer.class);
        Integer[] result = converter.parse("1,23", null);
        assertArrayEquals(new Integer[]{1, 23}, result);
    }

    @Test
    void testParseTwoElements_primitive() {
        String2ArrayConvertor<Integer, int[]> converter = new String2ArrayConvertor<>(int.class);
        int[] result = converter.parse("1,23", null);
        assertArrayEquals(new int[]{1, 23}, result);
    }

    @Test
    void testParseWithNullElement() {
        String2ArrayConvertor<Integer, Integer[]> converter = new String2ArrayConvertor<>(Integer.class);
        Integer[] result = converter.parse("1,,3", null);
        assertArrayEquals(new Integer[]{1, null, 3}, result);
    }

    @Test
    void testParseNull() {
        String2ArrayConvertor<Object, Object[]> converter = new String2ArrayConvertor<>(null);
        assertNull(converter.parse(null, null));
    }

    @Test
    void testParseNotIntegers() {
        assertThrows(NumberFormatException.class, () -> {
            String2ArrayConvertor<Integer, Integer[]> converter = new String2ArrayConvertor<>(Integer.class);
            converter.parse("12.30", null);
        });
    }

    @Test
    void testParseWrongValue() {
        assertThrows(NumberFormatException.class, () -> {
            String2ArrayConvertor<Integer, Integer[]> converter = new String2ArrayConvertor<>(Integer.class);
            converter.parse("12,34,_,56", null);
        });
    }
}
