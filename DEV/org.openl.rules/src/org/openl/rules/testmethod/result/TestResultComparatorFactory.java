package org.openl.rules.testmethod.result;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.openl.rules.helpers.NumberUtils;
import org.openl.types.IOpenField;

public class TestResultComparatorFactory {

    private TestResultComparatorFactory() {
    }

    public static TestResultComparator getComparator(Class<?> clazz, Double delta) {
        if (clazz.isArray()) {
            return new ArrayComparator(clazz.getComponentType(), delta);
        } else if (String.class.equals(clazz)) {
            return StringComparator.getInstance();
        } else if (NumberUtils.isFloatPointType(clazz)) {
            if (delta == null) {
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
