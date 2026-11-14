package org.openl.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class NumberUtilsTest {

    @ParameterizedTest
    @ValueSource(strings = {"0.5", "0.6666666666666666", "100.00", "1E+300", "-0.1", "0", "4.9E-324"})
    void holdsDecimalsOfDoublePrecision(String text) {
        assertTrue(NumberUtils.isHeldByDouble(new BigDecimal(text)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.66666666666666663", "0.6666666666666666666666666666666667", "1E+400", "1E-400",
            "12345678901234567890.5"})
    void doesNotHoldDecimalsOfMoreDigits(String text) {
        assertFalse(NumberUtils.isHeldByDouble(new BigDecimal(text)));
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1, 1L << 53, -(1L << 53)})
    void holdsWholeNumbersDoubleKeeps(long value) {
        assertTrue(NumberUtils.isHeldByDouble(value));
        assertTrue(NumberUtils.isHeldByDouble(BigInteger.valueOf(value)));
    }

    /** A whole number beyond 2^53 is held when its double is the number and the shortest decimal of it too. */
    @ParameterizedTest
    @ValueSource(longs = {1L << 54, 1_000_000_000_000_000_000L, -1_000_000_000_000_000_000L})
    void holdsLargeWholeNumbersBothReadsKeep(long value) {
        assertTrue(NumberUtils.isHeldByDouble(value));
        assertTrue(NumberUtils.isHeldByDouble(BigInteger.valueOf(value)));
        assertTrue(NumberUtils.isHeldByDouble(BigDecimal.valueOf(value)));
    }

    /**
     * A whole number is not held when a read of either type changes it: 1000000000000000100 has that shortest decimal,
     * but its double is 1000000000000000128, while the smallest long is a double whose shortest decimal is
     * -9223372036854776000.
     */
    @ParameterizedTest
    @ValueSource(longs = {1_000_000_000_000_000_100L, Long.MIN_VALUE, 3L << 60, -(1L << 62)})
    void doesNotHoldWholeNumbersOneReadChanges(long value) {
        assertFalse(NumberUtils.isHeldByDouble(value));
        assertFalse(NumberUtils.isHeldByDouble(BigInteger.valueOf(value)));
        assertFalse(NumberUtils.isHeldByDouble(BigDecimal.valueOf(value)));
    }

    @ParameterizedTest
    @ValueSource(doubles = {0, -0x1p63, 0x1p63 - 1024, 1e18, -1e18})
    void tellsDoublesInLongRange(double value) {
        assertTrue(NumberUtils.isInLongRange(value));
    }

    /** The largest long rounds up to 2^63 as a double, which is beyond the range. */
    @ParameterizedTest
    @ValueSource(doubles = {0x1p63, (double) Long.MAX_VALUE, -0x1p64, Double.NaN, Double.POSITIVE_INFINITY})
    void tellsDoublesBeyondLongRange(double value) {
        assertFalse(NumberUtils.isInLongRange(value));
    }

    @ParameterizedTest
    @ValueSource(longs = {(1L << 53) + 1, Long.MAX_VALUE, -(1L << 53) - 1})
    void doesNotHoldWholeNumbersDoubleRounds(long value) {
        assertFalse(NumberUtils.isHeldByDouble(value));
        assertFalse(NumberUtils.isHeldByDouble(BigInteger.valueOf(value)));
    }

    @ParameterizedTest
    @ValueSource(doubles = {Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY})
    void doesNotHoldNonFiniteNumbers(double value) {
        assertFalse(NumberUtils.isHeldByDouble(value));
        assertFalse(NumberUtils.isHeldByDouble((float) value));
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.1, 1.5, -3.0, -0.0, Double.MAX_VALUE, Double.MIN_VALUE})
    void holdsEveryFiniteDouble(double value) {
        assertTrue(NumberUtils.isHeldByDouble(value));
        assertTrue(NumberUtils.isHeldByDouble(BigDecimal.valueOf(value)));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.6666666666666666666666666666666667", " 9007199254740993 ", "1E999999999", "1E-400"})
    void readsTextOfNumberBeyondDouble(String text) {
        assertEquals(new BigDecimal(text.strip()), NumberUtils.beyondDouble(text));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.5", "100", "abc", "NaN", "", "1,000"})
    void readsNoNumberBeyondDoubleInOtherTexts(String text) {
        assertNull(NumberUtils.beyondDouble(text));
    }

    @Test
    void holdsSmallNumbers() {
        assertTrue(NumberUtils.isHeldByDouble(42));
        assertTrue(NumberUtils.isHeldByDouble(1.1f));
        assertTrue(NumberUtils.isHeldByDouble((short) 7));
    }

    @Test
    void isExactWholeOnlyForWholeNumbersBelowTwoToTheFiftyThird() {
        assertTrue(NumberUtils.isExactWhole(5100800601d));
        assertTrue(NumberUtils.isExactWhole(-0x1p53 + 1));
        assertFalse(NumberUtils.isExactWhole(0x1p53));
        assertFalse(NumberUtils.isExactWhole(1.5));
        assertFalse(NumberUtils.isExactWhole(Double.NaN));
        assertFalse(NumberUtils.isExactWhole(Double.POSITIVE_INFINITY));
    }
}
