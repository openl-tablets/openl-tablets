package org.openl.binding.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class NumericComparableStringTest {

    static Stream<Arguments> testIncrementAndGet() {
        return Stream.of(
                arguments("06400", "06401", "06402"),
                arguments("A00000", "A00001", "A00002"),
                arguments("A00000A", "A00000A" + Character.MIN_VALUE, "A00000B"),
                arguments("1111", "1112", "1113"),
                arguments("000.000", "000.001", "000.002"),
                arguments("000.009", "000.010", "000.011"),
                arguments("000.099", "000.100", "000.101"),
                arguments("000.999", "000.1000", "000.1001"));
    }

    @ParameterizedTest
    @MethodSource
    void testIncrementAndGet(String value, String expected, String greater) {
        var actual = increment(value);
        assertEquals(0, actual.compareTo(NumericComparableString.valueOf(expected)));
        assertEquals(expected, actual.toString());
        assertEquals(expected, actual.getValue());
        assertEquals(1, actual.compareTo(NumericComparableString.valueOf(value)));
        assertEquals(-1, actual.compareTo(NumericComparableString.valueOf(greater)));
    }

    static Stream<Arguments> testIncrement() {
        return Stream.of(
                arguments("abc8", "abc9"),
                arguments("abc08", "abc09"),
                arguments("abc9", "abc10"),
                arguments("abc", "abc\u0000"),
                arguments("", "\u0000"),
                arguments("8", "9"),
                arguments("9", "10"),
                arguments("09", "10"),
                arguments("009", "010"),
                arguments("99", "100"),
                arguments("99A", "99A\u0000"),
                arguments("99 ", "99 \u0000"),
                arguments("990", "991"),
                arguments("0", "1"),
                arguments("000", "001"));
    }

    @ParameterizedTest
    @MethodSource
    void testIncrement(String value, String expected) {
        assertEquals(NumericComparableString.valueOf(expected), increment(value));
    }

    static Stream<Arguments> testCompare() {
        return Stream.of(
                arguments("A07B", "A7A", 1),
                arguments("A07B", "A07A", 1),

                arguments("A07B", "A06", 1),
                arguments("A07B", "A6", 1),

                arguments("A07B", "A07", 1),
                arguments("A07B", "A7", 1),

                arguments("A07B", "A7B", 0),
                arguments("A07B", "A07B", 0),

                arguments("A07B", "A08", -1),
                arguments("A07B", "A8", -1),

                arguments("A07B", "A7C", -1),
                arguments("A07B", "A07C", -1),

                arguments("", "", 0),
                arguments(" ", "", 1),
                arguments("", " ", -1),

                arguments(" ", " ", 0),
                arguments(" ", "  ", -1),
                arguments("  ", " ", 1),

                arguments("0", "0", 0),
                arguments("0", "00", 0),
                arguments("00", "0", 0),

                arguments("A", "0", 1),
                arguments("0", "A", -1),
                arguments("AA", "A", 1),
                arguments("A", "AA", -1),
                arguments("A", "A", 0),

                arguments("0A", "00A", 0),
                arguments("0A", "A", -1),
                arguments("1A", "0A", 1),
                arguments("A", "0", 1),
                arguments("A0", "A00", 0),
                arguments("A1", "A01", 0),
                arguments("A1", "A10", -1),
                arguments("A2", "A10", -1),
                arguments("A2A", "A10", -1),
                arguments("A2A", "A1B", 1),
                arguments("2A2", "2A3", -1),
                arguments("2A20", "2A3", 1),
                arguments("20A4", "3A50", 1),
                arguments("005A4", "4B0", 1),
                arguments("0.0", "0.01", -1),
                arguments("0.10", "0.01", 1),
                arguments("01.2", "1.01", 1),
                arguments("01.02", "1.01", 1),
                arguments("01.002", "1.01", 1),
                arguments("01.002", ".01", 1),
                arguments("01A002", "A01", -1),
                arguments("0.1", "0.01", 0),

                arguments("0", "0 - 24", -1));
    }

    @ParameterizedTest
    @MethodSource
    void testCompare(String left, String right, int expectedSign) {
        assertEquals(expectedSign, Integer.signum(compare(left, right)));
    }

    private NumericComparableString increment(String value) {
        NumericComparableString origin = NumericComparableString.valueOf(value);
        var incremented = origin.incrementAndGet();
        assertTrue(compare(incremented, origin) > 0);
        return incremented;
    }

    private int compare(String left, String right) {
        NumericComparableString a = NumericComparableString.valueOf(left);
        NumericComparableString b = NumericComparableString.valueOf(right);
        return compare(a, b);
    }

    private int compare(NumericComparableString a, NumericComparableString b) {
        var result = a.compareTo(b);
        var inverse = b.compareTo(a);
        assertFalse(result < 0 && inverse <= 0);
        assertFalse(result > 0 && inverse >= 0);
        assertFalse(result == 0 && inverse != 0);
        if (a.equals(b)) {
            assertEquals(a, b);
            assertEquals(b, a);
            assertEquals(a.hashCode(), b.hashCode());
            assertEquals(0, result);
            assertEquals(0, inverse);
        }
        return result;
    }
}
