package org.openl.rules.testmethod.result;

import java.util.List;

import org.openl.rules.helpers.NumberUtils;
import org.openl.types.IOpenField;

public class TestResultComparatorFactory {
    private TestResultComparatorFactory(){}

    public static TestResultComparator getComparator(Object actualResult, Object expectedResult) {
        if (NumberUtils.isFloatPointNumber(actualResult) && NumberUtils.isFloatPointNumber(expectedResult)) {
            return new NumberComparator();
        }
        //Expected result and actual result can be different types (StubSpreadsheet)
        if (actualResult instanceof Comparable && (expectedResult == null || actualResult.getClass().equals(expectedResult.getClass()))) {
            return new ComparableResultComparator();
        }

        if (actualResult != null && expectedResult != null) {
            if (actualResult.getClass().isArray() && expectedResult.getClass().isArray()) {
                return new ArrayComparator();
            }
        }

        return new DefaultComparator();
    }

    public static TestResultComparator getOpenLBeanComparator(
            List<IOpenField> fieldsToTest) {
        return new BeanResultComparator(fieldsToTest);
    }
}
