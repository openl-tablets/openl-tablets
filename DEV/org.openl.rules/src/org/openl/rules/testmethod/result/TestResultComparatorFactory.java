package org.openl.rules.testmethod.result;

import java.util.Collection;
import java.util.Map;

import org.openl.rules.helpers.NumberUtils;

public class TestResultComparatorFactory {

    private TestResultComparatorFactory() {
    }

    public static TestResultComparator getComparator(Class<?> clazz, Double delta) {
        if (clazz == null) {
            GenericComparator.getInstance();
        } else if (clazz.isArray()) {
            return new ArrayComparator(clazz.getComponentType(), delta);
        } else if (String.class.equals(clazz)) {
            return StringComparator.getInstance();
        } else if (NumberUtils.isNumberType(clazz)) {
            if (delta == null) {
                if (NumberUtils.isNonFloatPointType(clazz)) {
                    //let's use Comparable comparator
                    return ComparableComparator.getInstance();
                }
                return NumberComparator.getInstance();
            } else {
                return new NumberComparator(delta);
            }
        } else if (Comparable.class.isAssignableFrom(clazz)) {
            // Expected result and actual result can be different types (StubSpreadsheet)
            return ComparableComparator.getInstance();
        } else if (Collection.class.isAssignableFrom(clazz)) {
            return CollectionComparator.getInstance();
        } else if (Map.class.isAssignableFrom(clazz)) {
            return MapComparator.getInstance();
        } else if (Object.class.equals(clazz)) {
            if (delta == null) {
                return ObjectComparator.getInstance();
            } else {
                return new ObjectComparator(delta);
            }
        }
        return GenericComparator.getInstance();
    }
}
