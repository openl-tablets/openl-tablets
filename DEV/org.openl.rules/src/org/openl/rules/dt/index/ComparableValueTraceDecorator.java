package org.openl.rules.dt.index;

public class ComparableValueTraceDecorator implements Comparable<Object> {
    protected final Object delegate;

    public ComparableValueTraceDecorator(Object delegate) {
        this.delegate = delegate;
    }

    @SuppressWarnings("unchecked")
    @Override
    public int compareTo(Object obj) {
        ComparableIndexTraceDecorator<Object> decorator = null;
        if (obj instanceof ComparableIndexTraceDecorator) {
            decorator = (ComparableIndexTraceDecorator<Object>) obj;
            obj = ((ComparableIndexTraceDecorator<Object>) obj).delegate;
        }

        Comparable<Object> c = (Comparable<Object>) delegate;
        int result = c.compareTo(obj);
        if (decorator != null) {
            decorator.traceComparisonResult(result == 0);
        }
        return result;
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object obj) {
        ComparableIndexTraceDecorator<Object> decorator = null;
        if (obj instanceof ComparableIndexTraceDecorator) {
            decorator = (ComparableIndexTraceDecorator<Object>) obj;
            obj = ((ComparableIndexTraceDecorator<Object>) obj).delegate;
        }
        boolean result = delegate.equals(obj);
        if (decorator != null) {
            decorator.traceComparisonResult(result);
        }
        return result;
    }

}
