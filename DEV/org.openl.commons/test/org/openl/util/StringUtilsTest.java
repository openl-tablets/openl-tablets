package org.openl.util;

import com.sun.xml.internal.fastinfoset.util.StringArray;
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
        String[] actual1 = StringUtils.split("",'*');
        String[] expected1 = {};
        String[] actual2 = StringUtils.split("a.b.c", '.');
        String[] expected2 = {"a", "b", "c"};
        String[] actual3 = StringUtils.split("a..b.c", '.');
        String[] actual4 = StringUtils.split("a:b:c", '.');
        String[] expected4 = {"a:b:c"};
        String[] actual5 = StringUtils.split("a b c", ' ');

        assertNull(StringUtils.split(null,' '));
        assertNull(StringUtils.split(null,'*'));
        assertArrayEquals("Returned array is not empty", expected1, actual1);
        assertArrayEquals("Returned array is not valid", expected2, actual2);
        assertArrayEquals("Returned array is not valid", expected2, actual3);
        assertArrayEquals("Returned array is not valid", expected4, actual4);
        assertArrayEquals("Returned array is not valid", expected2, actual5);
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
    public void testTrim() throws Exception {

    }

    @Test
    public void testTrimToNull() throws Exception {

    }

    @Test
    public void testTrimToEmpty() throws Exception {

    }

}