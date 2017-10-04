package org.openl.rules.testmethod.result;

import org.openl.rules.helpers.NumberUtils;

public class NumberComparator implements TestResultComparator {

    public boolean compareResult(Object actualResult, Object expectedResult, Double delta) {
        if (actualResult == null || expectedResult == null) {
            return actualResult == expectedResult;
        }
        Double actual = NumberUtils.convertToDouble(actualResult);
        Double expected = NumberUtils.convertToDouble(expectedResult);

        if (actual != null || expected != null) {
            if (Double.compare(actual, expected) == 0) {
                // NaN == NaN
                // +Inf == +Inf
                // -Inf == -Inf
                // Number == Number
                return true;
            } else if (Double.isInfinite(actual) || Double.isInfinite(expected) || Double.isNaN(actual) || Double.isNaN(expected)) {
                return false;
            } else {
                // Number ~= Number
                return Math.abs(actual - expected) <= (delta != null ? delta : Math.ulp(actual));
            }
        }
        return false;
    }
}
