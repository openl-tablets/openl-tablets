package org.openl.rules.testmethod.result;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.jspecify.annotations.Nullable;

import org.openl.rules.helpers.NumberUtils;

/**
 * Compares an expected number with an actual one, within a delta when the test gives a precision.
 * <p>
 * When either number is a {@link BigDecimal} or a {@link BigInteger}, both are compared with all their digits: they
 * are equal when the difference is within the delta, or is zero without a delta. A {@code Double} or a {@code Float}
 * that is NaN or infinite equals no such number. Other numbers are compared as {@code double}, with the delta rounded
 * to the nearest {@code double}, and without a delta they may differ by one unit in the last place of the actual
 * value. NaN equals NaN, and an infinity equals the same infinity only.
 * <p>
 * A delta below 1 allows a difference equal to it, a delta of 1 or more only a smaller difference.
 */
class NumberComparator implements TestResultComparator {

    static final TestResultComparator INSTANCE = new NumberComparator();

    private BigDecimal delta;

    /**
     * Use {@link #INSTANCE} instead.
     */
    private NumberComparator() {
    }

    NumberComparator(BigDecimal delta) {
        this.delta = delta;
    }

    @Override
    public boolean isEqual(Object expectedResult, Object actualResult) {
        if (actualResult == null || expectedResult == null) {
            return actualResult == expectedResult;
        }
        if (isExact(expectedResult) || isExact(actualResult)) {
            return isDecimalEqual(toDecimal(expectedResult), toDecimal(actualResult));
        }
        return isDoubleEqual(expectedResult, actualResult);
    }

    private boolean isDoubleEqual(Object expectedResult, Object actualResult) {
        Double actual = NumberUtils.convertToDouble(actualResult);
        Double expected = NumberUtils.convertToDouble(expectedResult);

        if (actual != null && expected != null) {
            if (Double.compare(actual, expected) == 0) {
                // NaN == NaN
                // +Inf == +Inf
                // -Inf == -Inf
                // Number == Number
                return true;
            } else if (Double.isInfinite(actual) || Double.isInfinite(expected) || Double.isNaN(actual) || Double
                    .isNaN(expected)) {
                return false;
            } else {
                // Number ~= Number
                var diff = Math.abs(actual - expected);
                double epsilon = delta == null ? Math.ulp(actual) : delta.doubleValue();
                return epsilon < 1 ? diff <= epsilon : diff < epsilon;
            }
        }
        return false;
    }

    private static boolean isExact(Object value) {
        return value instanceof BigDecimal || value instanceof BigInteger;
    }

    private boolean isDecimalEqual(@Nullable BigDecimal expected, @Nullable BigDecimal actual) {
        if (expected == null || actual == null) {
            return false;
        }
        var difference = expected.subtract(actual).abs();
        if (delta == null) {
            return difference.signum() == 0;
        }
        var order = difference.compareTo(delta);
        return delta.compareTo(BigDecimal.ONE) < 0 ? order <= 0 : order < 0;
    }

    /**
     * Returns the number with all its digits, or {@code null} for a value that is not a finite number.
     */
    private static @Nullable BigDecimal toDecimal(Object value) {
        return switch (value) {
            case BigDecimal decimal -> decimal;
            case BigInteger integer -> new BigDecimal(integer);
            case Double number -> Double.isFinite(number) ? new BigDecimal(number.toString()) : null;
            case Float number -> Float.isFinite(number) ? new BigDecimal(number.toString()) : null;
            case Byte number -> BigDecimal.valueOf(number);
            case Short number -> BigDecimal.valueOf(number);
            case Integer number -> BigDecimal.valueOf(number);
            case Long number -> BigDecimal.valueOf(number);
            default -> null;
        };
    }
}
