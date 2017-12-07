package org.openl.rules.testmethod.result;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.openl.rules.helpers.NumberUtils;
import org.openl.types.IOpenField;

public class TestResultComparatorFactory {

    private static final StringComparator STRING = new StringComparator();
    private static final NumberComparator NUMBER = new NumberComparator();
    private static final ComparableComparator<?> COMPARABLE = new ComparableComparator<>();
    private static final CollectionComparator COLLECTION = new CollectionComparator();
    private static final MapComparator MAP = new MapComparator();
    private static final GenericComparator<?> OBJECT = new GenericComparator<>();

    private TestResultComparatorFactory() {
    }

    public static TestResultComparator getComparator(Class<?> clazz) {
        if (clazz.isArray()) {
            return new ArrayComparator(clazz.getComponentType());
        } else if (clazz.equals(String.class)) {
            return STRING;
        } else if (NumberUtils.isFloatPointType(clazz)) {
            return NUMBER;
        } else if (clazz.isAssignableFrom(Comparable.class)) {
            // Expected result and actual result can be different types (StubSpreadsheet)
            return COMPARABLE;
        } else if (clazz.isAssignableFrom(Collection.class)) {
            return COLLECTION;
        } else if (clazz.isAssignableFrom(Map.class)) {
            return MAP;
        }

        return OBJECT;
    }

    public static TestResultComparator getOpenLBeanComparator(List<IOpenField> fieldsToTest) {
        return new BeanResultComparator(fieldsToTest);
    }
}
