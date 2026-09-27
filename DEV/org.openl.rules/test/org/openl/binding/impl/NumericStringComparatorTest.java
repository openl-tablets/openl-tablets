package org.openl.binding.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class NumericStringComparatorTest {

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
                arguments("0.1", "0.01", 0));
    }

    @ParameterizedTest
    @MethodSource
    void testCompare(String left, String right, int expectedSign) {
        assertEquals(expectedSign, Integer.signum(compare(left, right)));
    }

    private int compare(String a, String b) {
        var result = NumericStringComparator.INSTANCE.compare(a, b);
        var inverse = NumericStringComparator.INSTANCE.compare(b, a);
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
