package org.openl.binding.impl;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

public class NumericStringComparatorTest {

    @Test
    public void testCompare() {

        assertTrue(compare("A07B", "A7A") > 0);
        assertTrue(compare("A07B", "A07A")> 0);

        assertTrue(compare("A07B", "A06") > 0);
        assertTrue(compare("A07B", "A6") > 0);

        assertTrue(compare("A07B", "A07") > 0);
        assertTrue(compare("A07B", "A7") > 0);

        assertEquals(0, compare("A07B", "A7B"));
        assertEquals(0, compare("A07B", "A07B"));

        assertTrue(compare("A07B", "A08") < 0);
        assertTrue(compare("A07B", "A8") < 0);

        assertTrue(compare("A07B", "A7C") < 0);
        assertTrue(compare("A07B", "A07C") < 0);

        assertEquals(0, compare("", ""));
        assertTrue(compare(" ", "") > 0);
        assertTrue(compare("", " ") < 0);

        assertEquals(0, compare(" ", " "));
        assertTrue(compare(" ", "  ") < 0);
        assertTrue(compare("  ", " ") > 0);

        assertEquals(0, compare("0", "0"));
        assertEquals(0, compare("0", "00"));
        assertEquals(0, compare("00", "0"));

        assertTrue(compare("A", "0") > 0);
        assertTrue(compare("0", "A") < 0);
        assertTrue(compare("AA", "A") > 0);
        assertTrue(compare("A", "AA") < 0);
        assertEquals(0, compare("A", "A"));

        assertEquals(0, compare("0A", "00A"));
        assertTrue(compare("0A", "A") < 0);
        assertTrue(compare("1A", "0A") > 0);
        assertTrue(compare("A", "0") > 0);
        assertEquals(0, compare("A0", "A00"));
        assertEquals(0, compare("A1", "A01"));
        assertTrue(compare("A1", "A10") < 0);
        assertTrue(compare("A2", "A10") < 0);
        assertTrue(compare("A2A", "A10") < 0);
        assertTrue(compare("A2A", "A1B") > 0);
        assertTrue(compare("2A2", "2A3") < 0);
        assertTrue(compare("2A20", "2A3") > 0);
        assertTrue(compare("20A4", "3A50") > 0);
        assertTrue(compare("005A4", "4B0") > 0);
        assertTrue(compare("0.0", "0.01") < 0);
        assertTrue(compare("0.10", "0.01") > 0);
        assertTrue(compare("01.2", "1.01") > 0);
        assertTrue(compare("01.02", "1.01") > 0);
        assertTrue(compare("01.002", "1.01") > 0);
        assertTrue(compare("01.002", ".01") > 0);
        assertTrue(compare("01A002", "A01") < 0);
        assertEquals(0, compare("0.1", "0.01"));
    }

    private int compare(String a, String b) {
        int result = NumericStringComparator.INSTANCE.compare(a, b);
        int inverse = NumericStringComparator.INSTANCE.compare(b, a);
        assertFalse(result < 0 && inverse <= 0);
        assertFalse(result > 0 && inverse >= 0);
        assertFalse(result == 0 && inverse != 0);
        if (result == 0) {
            assertEquals(NumericStringComparator.hashCode(a), NumericStringComparator.hashCode(b));
            assertEquals(0, inverse);
        }
        return result;
    }

}
