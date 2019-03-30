package org.openl.rules.types.impl;

import org.openl.rules.table.properties.ITableProperties;

public abstract class IntersectionConstraint<P> {

    protected abstract IntersectionType matchNotNulls(P firstValue, P secondValue);

    protected abstract P getPropertyValue(ITableProperties secondProperties);

    public IntersectionType match(ITableProperties firstProperties, ITableProperties secondProperties) {
        P firstValue = getPropertyValue(firstProperties);
        P secondValue = getPropertyValue(secondProperties);

        return matchValues(firstValue, secondValue);
    }

    protected IntersectionType matchValues(P firstValue, P secondValue) {
        if (firstValue == secondValue) {
            return IntersectionType.EQUALS;
        }

        if (firstValue == null) {
            return IntersectionType.CONTAINS;
        }
        if (secondValue == null) {
            return IntersectionType.NESTED;
        }

        return matchNotNulls(firstValue, secondValue);
    }

    protected static <T> IntersectionType intersectionForLE(Comparable<T> firstValue, Comparable<T> secondValue) {
        @SuppressWarnings("unchecked")
        int comparison = firstValue.compareTo((T) secondValue);
        return comparison == 0 ? IntersectionType.EQUALS
                               : comparison < 0 ? IntersectionType.CONTAINS : IntersectionType.NESTED;
    }

    protected static <T> IntersectionType intersectionForGE(Comparable<T> firstValue, Comparable<T> secondValue) {
        @SuppressWarnings("unchecked")
        int comparison = firstValue.compareTo((T) secondValue);
        return comparison == 0 ? IntersectionType.EQUALS
                               : comparison > 0 ? IntersectionType.CONTAINS : IntersectionType.NESTED;
    }

    @SuppressWarnings("unchecked")
    protected static <T> IntersectionType intersectionForEQ(Comparable<T> firstValue, Comparable<T> secondValue) {
        return firstValue.compareTo((T) secondValue) == 0 ? IntersectionType.EQUALS : IntersectionType.NO_INTERSECTION;
    }

    protected static <T> IntersectionType intersectionForCONTAINS(Comparable<T>[] firstValue,
            Comparable<T>[] secondValue) {
        IntersectionType resultForNoAbsentElements;
        if (firstValue.length > secondValue.length) {
            resultForNoAbsentElements = IntersectionType.CONTAINS;

            Comparable<T>[] swap = firstValue;
            firstValue = secondValue;
            secondValue = swap;
        } else {
            resultForNoAbsentElements = firstValue.length < secondValue.length ? IntersectionType.NESTED
                                                                               : IntersectionType.EQUALS;
        }

        boolean hasEqualElements = false;
        boolean hasAbsentElements = false;
        for (Comparable<T> value : firstValue) {
            if (containsElement(secondValue, value)) {
                hasEqualElements = true;
            } else {
                hasAbsentElements = true;
            }
        }
        if (!hasAbsentElements) {
            return resultForNoAbsentElements;
        }

        return hasEqualElements ? IntersectionType.PARTLY_INTERSECTS : IntersectionType.NO_INTERSECTION;
    }

    @SuppressWarnings("unchecked")
    private static <T> boolean containsElement(Comparable<T>[] cmp1, Comparable<T> cmp2) {

        for (Comparable<T> element : cmp1) {
            if (element.compareTo((T) cmp2) == 0) {
                return true;
            }
        }

        return false;
    }
}
