package org.openl.rules.convertor;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNull;

public class String2ArrayConvertorTest {

    @Test
    public void testParseEmpty() {
        String2ArrayConvertor<Integer, Integer[]> converter = new String2ArrayConvertor<>(Integer.class);
        Integer[] result = converter.parse("", null);
        assertArrayEquals(new Integer[] {}, result);
    }

    @Test
    public void testParseSingleElement() {
        String2ArrayConvertor<Integer, Integer[]> converter = new String2ArrayConvertor<>(Integer.class);
        Integer[] result = converter.parse("123", null);
        assertArrayEquals(new Integer[] { 123 }, result);
    }

    @Test
    public void testParseTwoElements() {
        String2ArrayConvertor<Integer, Integer[]> converter = new String2ArrayConvertor<>(Integer.class);
        Integer[] result = converter.parse("1,23", null);
        assertArrayEquals(new Integer[] { 1, 23 }, result);
    }

    @Test
    public void testParseTwoElements_primitive() {
        String2ArrayConvertor<Integer, int[]> converter = new String2ArrayConvertor<>(int.class);
        int[] result = converter.parse("1,23", null);
        assertArrayEquals(new int[] { 1, 23 }, result);
    }

    @Test
    public void testParseWithNullElement() {
        String2ArrayConvertor<Integer, Integer[]> converter = new String2ArrayConvertor<>(Integer.class);
        Integer[] result = converter.parse("1,,3", null);
        assertArrayEquals(new Integer[] { 1, null, 3 }, result);
    }

    @Test
    public void testParseNull() {
        String2ArrayConvertor<Object, Object[]> converter = new String2ArrayConvertor<>(null);
        assertNull(converter.parse(null, null));
    }

    @Test(expected = NumberFormatException.class)
    public void testParseNotIntegers() {
        String2ArrayConvertor<Integer, Integer[]> converter = new String2ArrayConvertor<>(Integer.class);
        converter.parse("12.30", null);
    }

    @Test(expected = NumberFormatException.class)
    public void testParseWrongValue() {
        String2ArrayConvertor<Integer, Integer[]> converter = new String2ArrayConvertor<>(Integer.class);
        converter.parse("12,34,_,56", null);
    }
}
