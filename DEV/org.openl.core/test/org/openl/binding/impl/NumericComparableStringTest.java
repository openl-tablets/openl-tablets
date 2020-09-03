package org.openl.binding.impl;

import static junit.framework.TestCase.assertEquals;

import org.junit.Test;

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

}
