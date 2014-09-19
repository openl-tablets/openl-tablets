package org.openl.rules.dt.index;

import org.openl.rules.dt.trace.DTIndexedTraceObject;

import java.util.Comparator;

class ComparatorTraceDecorator implements Comparator<Object> {
    private Comparator<Object> delegate;

    @SuppressWarnings("unchecked")
    ComparatorTraceDecorator(Comparator<?> delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("delegate arg can't be null!");
        }
        this.delegate = (Comparator<Object>) delegate;
    }

    @Override
    public int compare(Object o1, Object o2) {
        Object p1 = o1;
        Object p2 = o2;
        if (o1 instanceof ComparableValueTraceDecorator) {
            p1 = ((ComparableValueTraceDecorator) o1).delegate;
        }
        if (o1 instanceof ComparableIndexTraceDecorator) {
            p1 = ((ComparableIndexTraceDecorator<?>) o1).delegate;
        }
        if (o1 instanceof ComparableValueTraceDecorator) {
            p2 = ((ComparableValueTraceDecorator) o1).delegate;
        }
        if (o2 instanceof ComparableIndexTraceDecorator) {
            p2 = ((ComparableIndexTraceDecorator<?>) o2).delegate;
        }
        int result = delegate.compare(p1, p2);
        return result;
    }
}
