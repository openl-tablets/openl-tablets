package org.openl.rules.testmethod.result;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.openl.rules.helpers.NumberUtils;
import org.openl.types.IOpenField;

public class TestResultComparatorFactory {

    private static final StringComparator STRING = new StringComparator();
    private static final NumberComparator NUMBER = new NumberComparator();
    private static final ComparableComparator COMPARABLE = new ComparableComparator();
    private static final CollectionComparator COLLECTION = new CollectionComparator();
    private static final MapComparator MAP = new MapComparator();
    private static final ObjectComparator OBJECT = new ObjectComparator();
    private static final GenericComparator GENERIC = new GenericComparator();

    private TestResultComparatorFactory() {
    }

    public static TestResultComparator getComparator(Class<?> clazz, Double delta) {
        if (clazz.isArray()) {
            return new ArrayComparator(clazz.getComponentType(), delta);
        } else if (String.class.equals(clazz)) {
            return STRING;
        } else if (NumberUtils.isFloatPointType(clazz)) {
            if (delta == null) {
                return NUMBER;
            } else {
                return new NumberComparator(delta);
            }
        } else if (Comparable.class.isAssignableFrom(clazz)) {
            // Expected result and actual result can be different types (StubSpreadsheet)
            return COMPARABLE;
        } else if (Collection.class.isAssignableFrom(clazz)) {
            return COLLECTION;
        } else if (Map.class.isAssignableFrom(clazz)) {
            return MAP;
        } else if (Object.class.equals(clazz)) {
            if (delta == null) {
                return OBJECT;
            } else {
                return new ObjectComparator(delta);
            }
        }

        return GENERIC;
    }

    public static TestResultComparator getOpenLBeanComparator(List<IOpenField> fieldsToTest) {
        return new BeanResultComparator(fieldsToTest);
    }
}
