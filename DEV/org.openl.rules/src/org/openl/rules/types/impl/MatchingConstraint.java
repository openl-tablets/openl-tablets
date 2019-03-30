package org.openl.rules.types.impl;

import java.lang.reflect.Array;

import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.table.properties.ITableProperties;

public abstract class MatchingConstraint<P, C> {

    public MatchingResult match(ITableProperties properties, IRulesRuntimeContext context) {

        C contextValue = getContextValue(context);
        P propertyValue = getPropertyValue(properties);

        return matchValues(propertyValue, contextValue);
    }

    public MatchingResult matchValues(P propertyValue, C contextValue) {

        if (propertyValue == null || contextValue == null) {
            return MatchingResult.MATCH_BY_DEFAULT;
        }

        if (propertyValue.getClass().isArray() && Array.getLength(propertyValue) == 0) {
            return MatchingResult.MATCH_BY_DEFAULT;
        }

        if (matchNotNulls(propertyValue, contextValue)) {
            return MatchingResult.MATCH;
        }

        return MatchingResult.NO_MATCH;
    }

    protected abstract P getPropertyValue(ITableProperties properties);

    protected abstract C getContextValue(IRulesRuntimeContext context);

    protected abstract boolean matchNotNulls(P propValue, C contextValue);

    @SuppressWarnings("unchecked")
    public static <T> boolean LE(Comparable<T> cmp1, Comparable<T> cmp2) {
        return cmp1.compareTo((T) cmp2) <= 0;
    }

    @SuppressWarnings("unchecked")
    public static <T> boolean GE(Comparable<T> cmp1, Comparable<T> cmp2) {
        return cmp1.compareTo((T) cmp2) >= 0;
    }

    @SuppressWarnings("unchecked")
    public static <T> boolean EQ(Comparable<T> cmp1, Comparable<T> cmp2) {
        return cmp1.compareTo((T) cmp2) == 0;
    }

    @SuppressWarnings("unchecked")
    public static <T> boolean CONTAINS(Comparable<T>[] cmp1, Comparable<T> cmp2) {

        for (Comparable<T> element : cmp1) {
            if (element.compareTo((T) cmp2) == 0) {
                return true;
            }
        }

        return false;
    }

}
