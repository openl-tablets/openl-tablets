package org.openl.rules.convertor;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class String2ArrayConvertorTest {

    @Test
    public void testParseEmpty() {
        String2ArrayConvertor<Integer, Integer[]> converter = new String2ArrayConvertor(Integer.class);
        Integer[] result = converter.parse("", null);
        assertArrayEquals(new Integer[]{}, result);
    }

    @Test
    public void testFormatEmpty() {
        String2ArrayConvertor<Integer, Integer[]> converter = new String2ArrayConvertor(Integer.class);
        String result = converter.format(new Integer[]{}, null);
        assertEquals("", result);
    }

    @Test
    public void testParseSingleElement() {
        String2ArrayConvertor<Integer, Integer[]> converter = new String2ArrayConvertor(Integer.class);
        Integer[] result = converter.parse("123", null);
        assertArrayEquals(new Integer[]{123}, result);
    }

    @Test
    public void testFormatSingleElement() {
        String2ArrayConvertor<Integer, Integer[]> converter = new String2ArrayConvertor(Integer.class);
        String result = converter.format(new Integer[]{456}, null);
        assertEquals("456", result);
    }

    @Test
    public void testParseTwoElements() {
        String2ArrayConvertor<Integer, Integer[]> converter = new String2ArrayConvertor(Integer.class);
        Integer[] result = converter.parse("1,23", null);
        assertArrayEquals(new Integer[]{1,23}, result);
    }

    @Test
    public void testParseTwoElements_primitive() {
        String2ArrayConvertor<Integer, int[]> converter = new String2ArrayConvertor(int.class);
        int[] result = converter.parse("1,23", null);
        assertArrayEquals(new int[]{1,23}, result);
    }

    @Test
    public void testFormatTwoElements() {
        String2ArrayConvertor<Integer, Integer[]> converter = new String2ArrayConvertor(Integer.class);
        String result = converter.format(new Integer[]{45,6}, null);
        assertEquals("45,6", result);
    }

    @Test
    public void testParseWithNullElement() {
        String2ArrayConvertor<Integer, Integer[]> converter = new String2ArrayConvertor(Integer.class);
        Integer[] result = converter.parse("1,,3", null);
        assertArrayEquals(new Integer[]{1,null,3}, result);
    }

    @Test
    public void testFormatWithNullElement() {
        String2ArrayConvertor<Integer, Integer[]> converter = new String2ArrayConvertor(Integer.class);
        String result = converter.format(new Integer[]{4,null,6}, null);
        assertEquals("4,,6", result);
    }

    @Test
    public void testParseNull() {
        String2ArrayConvertor converter = new String2ArrayConvertor(null);
        assertNull(converter.parse(null, null));
    }

    @Test
    public void testFormatNull() {
        String2ArrayConvertor converter = new String2ArrayConvertor(null);
        assertNull(converter.format(null, null));
    }

    @Test(expected = NumberFormatException.class)
    public void testParseNotIntegers() {
        String2ArrayConvertor<Integer, Integer[]> converter = new String2ArrayConvertor(Integer.class);
        converter.parse("12.30", null);
    }

    @Test(expected = NumberFormatException.class)
    public void testParseWrongValue() {
        String2ArrayConvertor<Integer, Integer[]> converter = new String2ArrayConvertor(Integer.class);
        converter.parse("12,34,_,56", null);
    }
}
