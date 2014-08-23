package org.openl.rules.dt.index;

import java.util.Comparator;

import org.openl.rules.dt.trace.DTIndexedTraceObject;

class ComparatorTraceDecorator implements Comparator<Object> {
    private Comparator<Object> delegate;

    @SuppressWarnings("unchecked")
    ComparatorTraceDecorator(Comparator<?> delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException("delegate arg can't be null!");
        }
        this.delegate = (Comparator<Object>)delegate;
    }

    @Override
    public int compare(Object o1, Object o2) {
        Object p1 = o1;
        Object p2 = o2;
        if (o1 instanceof ComparableValueTraceDecorator) {
            p1 = ((ComparableValueTraceDecorator) o1).delegate;
        }
        if (o1 instanceof ComparableIndexTraceDecorator){
            p1 = ((ComparableIndexTraceDecorator<?>) o1).delegate;
        }
        if (o1 instanceof ComparableValueTraceDecorator) {
            p2 = ((ComparableValueTraceDecorator) o1).delegate;
        }
        if (o2 instanceof ComparableIndexTraceDecorator){
            p2 = ((ComparableIndexTraceDecorator<?>) o2).delegate;
        }
        int result = delegate.compare(p1, p2);
        if (o1 instanceof ComparableValueTraceDecorator) {
            traceComparisonResult(result == 0, o2);
        } else {
            if (o2 instanceof ComparableValueTraceDecorator) {
                traceComparisonResult(result == 0, o1);
            } 
        }
        return result;
    }

    public void traceComparisonResult(boolean successful, Object o) {
        if (!(o instanceof ComparableIndexTraceDecorator)) {
            return;
        }
        @SuppressWarnings("unchecked")
        ComparableIndexTraceDecorator<Object> comparableIndexTraceDecorator = (ComparableIndexTraceDecorator<Object>) o;
        if (!comparableIndexTraceDecorator.linkedRule.getRulesIterator().hasNext()) {
            // Do not trace index value that is not mapped to any rule. This can
            // be an excluding boundary for example.
            return;
        }
        // TODO: remove side effect from the push method
        comparableIndexTraceDecorator.traceStack.push(new DTIndexedTraceObject(comparableIndexTraceDecorator.baseTraceObject,
            comparableIndexTraceDecorator.condition,
            comparableIndexTraceDecorator.linkedRule,
            successful));

        if (!successful) {
            comparableIndexTraceDecorator.traceStack.pop();
        }
    }
}
