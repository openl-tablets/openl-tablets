package org.openl.util;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by tsaltsevich on 5/3/2016.
 */
public class StringUtilsTest {
    @Test
    public void testToBytes() throws Exception {
    }

    @Test
    public void testSplit() throws Exception {

        assertNull(StringUtils.split(null));
        assertArrayEquals("Returned array is not empty", new String[] {}, StringUtils.split(""));
        assertArrayEquals("Returned array is not valid", new String[] {"abc", "def"}, StringUtils.split("abc def"));
        assertArrayEquals("Returned array is not valid", new String[] {"abc", "def"}, StringUtils.split("abc   def"));
        assertArrayEquals("Returned array is not valid", new String[] {"abc"}, StringUtils.split(" abc "));
        assertArrayEquals("Returned array is not valid", new String[] {"abc", "def"}, StringUtils.split(" abc def "));
        assertArrayEquals("Returned array is not valid", new String[] {"abc", "def"}, StringUtils.split("abc\tdef"));
        assertArrayEquals("Returned array is not valid", new String[] {"abc", "def"}, StringUtils.split("\tabc\t\tdef"));
        assertArrayEquals("Returned array is not valid", new String[] {"abc", "def"}, StringUtils.split("\t\t  \tabc \t\t def "));
        assertArrayEquals("Returned array is not valid", new String[] {"abc", "def"}, StringUtils.split("abc\t \r\ndef"));
    }

    @Test
    public void testSplit2() throws Exception {

        assertNull(StringUtils.split(null,' '));
        assertNull(StringUtils.split(null,'*'));
        assertArrayEquals("Returned array is not empty", new String[] {}, StringUtils.split("",'*'));
        assertArrayEquals("Returned array is not valid", new String[] {"a", "b", "c"}, StringUtils.split("a.b.c", '.'));
        assertArrayEquals("Returned array is not valid", new String[] {"a", "b", "c"}, StringUtils.split("a..b.c", '.'));
        assertArrayEquals("Returned array is not valid", new String[] {"a:b:c"}, StringUtils.split("a:b:c", '.'));
        assertArrayEquals("Returned array is not valid", new String[] {"a", "b", "c"}, StringUtils.split("a b c", ' '));
        assertArrayEquals("Returned array is not valid", new String[] {"a", "b", "c"}, StringUtils.split("a..b.c.", '.'));
        assertArrayEquals("Returned array is not valid", new String[] {"a", "b", "c"}, StringUtils.split("..a..b.c..", '.'));
        assertArrayEquals("Returned array is not valid", new String[] {"a"}, StringUtils.split("a..", '.'));
        assertArrayEquals("Returned array is not valid", new String[] {"a"}, StringUtils.split("a.", '.'));
        assertArrayEquals("Returned array is not valid", new String[] {"a"}, StringUtils.split(".a", '.'));
        assertArrayEquals("Returned array is not valid", new String[] {"a"}, StringUtils.split("..a", '.'));
        assertArrayEquals("Returned array is not valid", new String[] {"a"}, StringUtils.split("..a.", '.'));
        assertArrayEquals("Returned array is not valid", new String[] {"a"}, StringUtils.split("..a..", '.'));
    }

    @Test
    public void testJoinArray() throws Exception {

    }

    @Test
    public void testJoinItterable() throws Exception {

    }

    @Test
    public void testIsEmpty() throws Exception {

    }

    @Test
    public void testIsNotEmpty() throws Exception {

    }

    @Test
    public void testIsBlank() throws Exception {

    }

    @Test
    public void testIsNotBlank() throws Exception {

    }

    @Test
    public void testEquals() throws Exception {

    }

    @Test
    public void testNotEquals() throws Exception {

    }

    @Test
    public void testContainsIgnoreCase() throws Exception {

    }

    @Test
    public void testTrim() throws Exception {

    }

    @Test
    public void testTrimToNull() throws Exception {

    }

    @Test
    public void testTrimToEmpty() throws Exception {

    }

    @Test
    public void testRemoveStart() throws Exception {

    }

    @Test
    public void testRemoveEnd() throws Exception {

    }

    @Test
    public void testCapitalize() throws Exception {

    }

    @Test
    public void testUncapitalize() throws Exception {

    }
}
