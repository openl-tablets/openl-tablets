package org.openl.rules.dt.index;

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
        Object p1 = unwrap(o1);
        Object p2 = unwrap(o2);
        int result = delegate.compare(p1, p2);
        return result;
    }

    private Object unwrap(Object value) {
        Object result;
        if (value instanceof ComparableValueTraceDecorator) {
            result = ((ComparableValueTraceDecorator) value).delegate;
        } else if (value instanceof ComparableIndexTraceDecorator) {
            result = ((ComparableIndexTraceDecorator<?>) value).delegate;
        } else {
            result = value;
        }
        return result;
    }
}
