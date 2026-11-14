package org.openl.rules.testmethod.result;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.openl.rules.helpers.NumberUtils;

/**
 * Compares an expected number with an actual one to significant figures of the expected number.
 * <p>
 * Both numbers are rounded half up at the last significant figure of the expected number, and they must be equal: at
 * its {@code N}th figure for {@code Nsf}, or at the last figure it is written with for {@code sf}. So {@code 99.00} in
 * 4 figures matches {@code 98.995}, but not {@code 99.005}. A half rounds away from zero.
 * <p>
 * Without the written text, the expected number is taken as it reads: all the digits of a {@code BigDecimal}, the
 * shortest form of a {@code Double} or a {@code Float}, every digit of a whole number. The numbers are compared as
 * decimals, with all the digits they show, and only zero agrees with zero in {@code N} figures. NaN equals NaN, and an
 * infinity equals the same infinity only.
 *
 * @author Yury Molchan
 */
class FiguresComparator implements TestResultComparator {

    private final Tolerance.Figures figures;

    FiguresComparator(Tolerance.Figures figures) {
        this.figures = figures;
    }

    @Override
    public boolean isEqual(Object expectedResult, Object actualResult) {
        if (actualResult == null || expectedResult == null) {
            return actualResult == expectedResult;
        }
        var expected = NumberComparator.toDecimal(expectedResult);
        var actual = NumberComparator.toDecimal(actualResult);
        if (expected == null || actual == null) {
            // NaN == NaN, +Inf == +Inf, -Inf == -Inf; a finite number beyond the double range is no infinity
            if (expected != null || actual != null) {
                return false;
            }
            var expectedNumber = NumberUtils.convertToDouble(expectedResult);
            var actualNumber = NumberUtils.convertToDouble(actualResult);
            return expectedNumber != null && actualNumber != null && Double.compare(expectedNumber, actualNumber) == 0;
        }
        if (figures instanceof Tolerance.SignificantFigures && expected.signum() == 0) {
            // zero has no significant figures
            return actual.signum() == 0;
        }
        long scale = switch (figures) {
            case Tolerance.SignificantFigures(int count) -> count - 1 - exponent(expected);
            case Tolerance.ExpectedFigures(Integer written) ->
                written == null ? lastScale(expectedResult, expected) : written;
        };
        return isEqualAt(expected, actual, scale);
    }

    /**
     * Tells whether two numbers rounded half up at the decimal place {@code scale} are equal.
     */
    private static boolean isEqualAt(BigDecimal expected, BigDecimal actual, long scale) {
        if (scale >= Math.max(expected.scale(), actual.scale())) {
            // the rounding changes neither number
            return expected.compareTo(actual) == 0;
        } else if (-scale > Math.max(exponent(expected), exponent(actual)) + 1) {
            // both numbers are below a tenth of the unit, so both round to zero
            return true;
        }
        return expected.setScale((int) scale, RoundingMode.HALF_UP)
                .compareTo(actual.setScale((int) scale, RoundingMode.HALF_UP)) == 0;
    }

    /**
     * Returns the power of ten of the first significant figure of a number: 1 for {@code 10.00}, -4 for
     * {@code 0.0001}.
     */
    private static long exponent(BigDecimal number) {
        return (long) number.precision() - number.scale() - 1;
    }

    /**
     * Returns the decimal place of the last figure of a number as it reads: the shortest form of a {@code Double} or a
     * {@code Float}, the digits of a {@code BigDecimal}, and the units of a whole number. A whole number counts every
     * digit, even when its trailing zeros were dropped, as a {@code BigDecimal} read from a cell drops them.
     */
    private static int lastScale(Object value, BigDecimal number) {
        var decimal = value instanceof Double || value instanceof Float ? number.stripTrailingZeros() : number;
        return Math.max(decimal.scale(), 0);
    }
}
