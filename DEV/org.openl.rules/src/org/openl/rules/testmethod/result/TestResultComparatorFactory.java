package org.openl.rules.testmethod.result;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import org.openl.rules.helpers.NumberUtils;
import org.openl.util.ClassUtils;

public class TestResultComparatorFactory {

    private TestResultComparatorFactory() {
    }

    public static TestResultComparator getComparator(@Nullable Class<?> clazz, @Nullable Tolerance tolerance) {
        if (clazz == null) {
            return GenericComparator.INSTANCE;
        } else if (clazz.isArray()) {
            return new ArrayComparator(clazz.getComponentType(), tolerance);
        } else if (String.class == clazz) {
            return StringComparator.INSTANCE;
        } else if (NumberUtils.isNumberType(clazz)) {
            return getNumberComparator(clazz, tolerance);
        } else if (ClassUtils.isAssignable(clazz, Comparable.class)) {
            // Expected result and actual result can be different types (StubSpreadsheet)
            return ComparableComparator.INSTANCE;
        } else if (ClassUtils.isAssignable(clazz, Collection.class)) {
            return CollectionComparator.INSTANCE;
        } else if (ClassUtils.isAssignable(clazz, Map.class)) {
            return MapComparator.INSTANCE;
        } else if (Object.class == clazz || Serializable.class == clazz) {
            return tolerance == null ? ObjectComparator.INSTANCE : new ObjectComparator(tolerance);
        }
        return GenericComparator.INSTANCE;
    }

    private static TestResultComparator getNumberComparator(Class<?> clazz, @Nullable Tolerance tolerance) {
        return switch (tolerance) {
            // let's use Comparable comparator
            case null -> NumberUtils.isNonFloatPointType(clazz) ? ComparableComparator.INSTANCE
                    : NumberComparator.INSTANCE;
            case Tolerance.Delta(BigDecimal value) -> new NumberComparator(value);
            case Tolerance.Figures figures -> new FiguresComparator(figures);
        };
    }
}
