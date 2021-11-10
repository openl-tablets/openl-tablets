package org.openl.binding.impl;

import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

public class NumericComparableStringTest {

    @Test
    public void testIncrementAndGet() {
        NumericComparableString numericString = NumericComparableString.valueOf("06400");

        NumericComparableString actual = numericString.incrementAndGet();
        assertEquals(0, actual.compareTo(NumericComparableString.valueOf("06401")));
        assertEquals("06401", actual.toString());
        assertEquals("06401", actual.getValue());
        assertEquals(1, actual.compareTo(NumericComparableString.valueOf("06400")));
        assertEquals(-1, actual.compareTo(NumericComparableString.valueOf("06402")));

        numericString = NumericComparableString.valueOf("A00000");
        actual = numericString.incrementAndGet();
        assertEquals(0, actual.compareTo(NumericComparableString.valueOf("A00001")));
        assertEquals("A00001", actual.toString());
        assertEquals("A00001", actual.getValue());
        assertEquals(1, actual.compareTo(NumericComparableString.valueOf("A00000")));
        assertEquals(-1, actual.compareTo(NumericComparableString.valueOf("A00002")));

        numericString = NumericComparableString.valueOf("A00000A");
        actual = numericString.incrementAndGet();
        String expectedStringValue = "A00000A" + Character.MIN_VALUE;
        assertEquals(0, actual.compareTo(NumericComparableString.valueOf(expectedStringValue)));
        assertEquals(expectedStringValue, actual.toString());
        assertEquals(expectedStringValue, actual.getValue());
        assertEquals(1, actual.compareTo(NumericComparableString.valueOf("A00000A")));
        assertEquals(-1, actual.compareTo(NumericComparableString.valueOf("A00000B")));

        numericString = NumericComparableString.valueOf("1111");
        actual = numericString.incrementAndGet();
        assertEquals(0, actual.compareTo(NumericComparableString.valueOf("1112")));
        assertEquals("1112", actual.toString());
        assertEquals("1112", actual.getValue());
        assertEquals(1, actual.compareTo(NumericComparableString.valueOf("1111")));
        assertEquals(-1, actual.compareTo(NumericComparableString.valueOf("1113")));

        numericString = NumericComparableString.valueOf("000.000");
        actual = numericString.incrementAndGet();
        assertEquals(0, actual.compareTo(NumericComparableString.valueOf("000.001")));
        assertEquals("000.001", actual.toString());
        assertEquals("000.001", actual.getValue());
        assertEquals(1, actual.compareTo(NumericComparableString.valueOf("000.000")));
        assertEquals(-1, actual.compareTo(NumericComparableString.valueOf("000.002")));

        numericString = NumericComparableString.valueOf("000.009");
        actual = numericString.incrementAndGet();
        assertEquals(0, actual.compareTo(NumericComparableString.valueOf("000.010")));
        assertEquals("000.010", actual.toString());
        assertEquals("000.010", actual.getValue());
        assertEquals(1, actual.compareTo(NumericComparableString.valueOf("000.009")));
        assertEquals(-1, actual.compareTo(NumericComparableString.valueOf("000.011")));

        numericString = NumericComparableString.valueOf("000.099");
        actual = numericString.incrementAndGet();
        assertEquals(0, actual.compareTo(NumericComparableString.valueOf("000.100")));
        assertEquals("000.100", actual.toString());
        assertEquals("000.100", actual.getValue());
        assertEquals(1, actual.compareTo(NumericComparableString.valueOf("000.099")));
        assertEquals(-1, actual.compareTo(NumericComparableString.valueOf("000.101")));

        numericString = NumericComparableString.valueOf("000.999");
        actual = numericString.incrementAndGet();
        assertEquals(0, actual.compareTo(NumericComparableString.valueOf("000.1000")));
        assertEquals("000.1000", actual.toString());
        assertEquals("000.1000", actual.getValue());
        assertEquals(1, actual.compareTo(NumericComparableString.valueOf("000.999")));
        assertEquals(-1, actual.compareTo(NumericComparableString.valueOf("000.1001")));
    }

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

    private int compare(String left, String right) {
        NumericComparableString a = NumericComparableString.valueOf(left);
        NumericComparableString b = NumericComparableString.valueOf(right);
        int result = a.compareTo(b);
        int inverse = b.compareTo(a);
        assertFalse(result < 0 && inverse <= 0);
        assertFalse(result > 0 && inverse >= 0);
        assertFalse(result == 0 && inverse != 0);
        return result;
    }
}
