package org.openl.rules.testmethod.result;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

import org.openl.rules.helpers.NumberUtils;
import org.openl.util.ClassUtils;

public class TestResultComparatorFactory {

    private TestResultComparatorFactory() {
    }

    public static TestResultComparator getComparator(Class<?> clazz, BigDecimal delta) {
        if (clazz == null) {
            return GenericComparator.INSTANCE;
        } else if (clazz.isArray()) {
            return new ArrayComparator(clazz.getComponentType(), delta);
        } else if (String.class == clazz) {
            return StringComparator.INSTANCE;
        } else if (NumberUtils.isNumberType(clazz)) {
            return getNumberComparator(clazz, delta);
        } else if (ClassUtils.isAssignable(clazz, Comparable.class)) {
            // Expected result and actual result can be different types (StubSpreadsheet)
            return ComparableComparator.INSTANCE;
        } else if (ClassUtils.isAssignable(clazz, Collection.class)) {
            return CollectionComparator.INSTANCE;
        } else if (ClassUtils.isAssignable(clazz, Map.class)) {
            return MapComparator.INSTANCE;
        } else if (Object.class == clazz || Serializable.class == clazz) {
            if (delta == null) {
                return ObjectComparator.INSTANCE;
            } else {
                return new ObjectComparator(delta);
            }
        }
        return GenericComparator.INSTANCE;
    }

    private static TestResultComparator getNumberComparator(Class<?> clazz, BigDecimal delta) {
        if (delta == null) {
            if (NumberUtils.isNonFloatPointType(clazz)) {
                // let's use Comparable comparator
                return ComparableComparator.INSTANCE;
            }
            return NumberComparator.INSTANCE;
        } else {
            return new NumberComparator(delta);
        }
    }
}
