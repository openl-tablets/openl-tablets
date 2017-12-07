package org.openl.rules.testmethod.result;

import java.util.List;

import org.openl.rules.helpers.NumberUtils;
import org.openl.types.IOpenField;

public class TestResultComparatorFactory {
    private TestResultComparatorFactory(){}

    public static TestResultComparator getComparator(Class<?> clazz) {
        if (NumberUtils.isFloatPointType(clazz)) {
            return new NumberComparator();
        }
        //Expected result and actual result can be different types (StubSpreadsheet)
        if (clazz.isAssignableFrom(Comparable.class)) {
            return new ComparableResultComparator();
        }

        if (clazz.isArray()) {
            return new ArrayComparator(clazz.getComponentType());
        }

        return new DefaultComparator();
    }

    public static TestResultComparator getOpenLBeanComparator(
            List<IOpenField> fieldsToTest) {
        return new BeanResultComparator(fieldsToTest);
    }
}
