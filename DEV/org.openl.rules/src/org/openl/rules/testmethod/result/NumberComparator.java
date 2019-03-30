package org.openl.rules.testmethod.result;

import org.openl.rules.helpers.NumberUtils;

class NumberComparator implements TestResultComparator {

    private static final NumberComparator INSTANCE = new NumberComparator();

    private Double delta;

    /**
     * Use {@link #getInstance()} instead.
     */
    private NumberComparator() {
    }

    NumberComparator(Double delta) {
        this.delta = delta;
    }

    @Override
    public boolean isEqual(Object expectedResult, Object actualResult) {
        if (actualResult == null || expectedResult == null) {
            return actualResult == expectedResult;
        }
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
                return Math.abs(actual - expected) <= (delta != null ? delta : Math.ulp(actual));
            }
        }
        return false;
    }

    public static TestResultComparator getInstance() {
        return INSTANCE;
    }
}
