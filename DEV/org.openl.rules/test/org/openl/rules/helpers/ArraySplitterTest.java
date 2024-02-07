package org.openl.rules.helpers;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ArraySplitterTest {

    @Test
    public void split() {
        assertArrayEquals(new String[0], ArraySplitter.split(""));
        assertArrayEquals(new String[0], ArraySplitter.split("   \t\r\n  \b "));
        assertArrayEquals(new String[]{"test"}, ArraySplitter.split("test"));
        assertArrayEquals(new String[]{"t e s t"}, ArraySplitter.split(" t e s t "));
        assertArrayEquals(new String[]{","}, ArraySplitter.split(" \\,  "));
        assertArrayEquals(new String[]{","}, ArraySplitter.split("\\,"));
        assertArrayEquals(new String[]{",\\,"}, ArraySplitter.split("\\,\\\\,"));
        assertArrayEquals(new String[]{null, ",\\,"}, ArraySplitter.split(",\\,\\\\,"));
        assertArrayEquals(new String[3], ArraySplitter.split(",,"));
        assertArrayEquals(new String[2], ArraySplitter.split(","));
        assertArrayEquals(new String[2], ArraySplitter.split("  \t\r\n\b , \t\r\n\b"));
        assertArrayEquals(new String[]{","}, ArraySplitter.split("  \t\r\n\b \\, \t\r\n\b"));
        assertArrayEquals(new String[]{"a", "s"}, ArraySplitter.split("a,s"));
        assertArrayEquals(new String[]{null, "a", "s", null}, ArraySplitter.split(",a,s,"));
        assertArrayEquals(new String[]{"s", null, null, "a"}, ArraySplitter.split("s, ,,a"));
        assertArrayEquals(new String[]{"a \\", "s"}, ArraySplitter.split(" a \\ , s "));
    }

    @Test
    public void isArray() {
        assertFalse(ArraySplitter.isArray(""));
        assertFalse(ArraySplitter.isArray("   \t\r\n \b "));
        assertFalse(ArraySplitter.isArray("test"));
        assertFalse(ArraySplitter.isArray(" t e s t "));
        assertTrue(ArraySplitter.isArray(" \\,  "));
        assertTrue(ArraySplitter.isArray("\\,"));
        assertTrue(ArraySplitter.isArray("\\,\\\\,"));
        assertTrue(ArraySplitter.isArray(",\\,\\\\,"));
        assertTrue(ArraySplitter.isArray(",,"));
        assertTrue(ArraySplitter.isArray(","));
        assertTrue(ArraySplitter.isArray("  \t\r\n\b , \t\r\n\b"));
        assertTrue(ArraySplitter.isArray("  \t\r\n\b \\, \t\r\n\b"));
        assertTrue(ArraySplitter.isArray("a,s"));
        assertTrue(ArraySplitter.isArray(",a,s,"));
        assertTrue(ArraySplitter.isArray("s, ,,a"));
        assertTrue(ArraySplitter.isArray(" a \\ , s "));
    }

    @Test
    public void testSplitAndEscape() {
        assertArrayEquals(new String[]{"Hello! I want to split it", "Right", "Lets Do it"}, ArraySplitter.split("Hello! I want to split it, Right , Lets Do it"));
        assertArrayEquals(new String[]{"Hello! I want to split it", "Right", "Lets Do it"}, ArraySplitter.split("    Hello! I want to split it, Right , Lets Do it    "));
        assertArrayEquals(new String[]{"Hello! I want to split it, Right", "Lets Do it"}, ArraySplitter.split("Hello! I want to split it\\, Right,Lets Do it"));
        assertArrayEquals(new String[]{"12", "23", "34"}, ArraySplitter.split("12,23,34"));
        assertArrayEquals(new String[]{"12,23,34"}, ArraySplitter.split("12\\,23\\,34"));
        assertArrayEquals(new String[]{"12,23,34", "456"}, ArraySplitter.split("12\\,23\\,34,456"));
        assertArrayEquals(new String[]{"Spencer, Sara's Son", "Sara"}, ArraySplitter.split("Spencer\\, Sara's Son, Sara"));
        assertArrayEquals(new String[]{"Trucks, Tractors, And Trailers Zone Rated"}, ArraySplitter.split("Trucks\\, Tractors\\, And Trailers Zone Rated"));
    }
}
