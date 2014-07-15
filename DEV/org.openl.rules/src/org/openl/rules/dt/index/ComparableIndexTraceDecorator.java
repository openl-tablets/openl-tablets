package org.openl.rules.dt.index;

import org.openl.rules.dt.DecisionTableRuleNode;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.trace.DTIndexedTraceObject;
import org.openl.rules.dt.trace.DecisionTableTraceObject;
import org.openl.vm.trace.TraceStack;

public class ComparableIndexTraceDecorator<T> implements Comparable<T> {
    protected final Comparable<T> delegate;
    private final DecisionTableRuleNode linkedRule;
    private final ICondition condition;
    private final DecisionTableTraceObject baseTraceObject;
    private final TraceStack traceStack;

    public ComparableIndexTraceDecorator(Comparable<T> delegate, DecisionTableRuleNode linkedRule,
            ICondition condition, DecisionTableTraceObject baseTraceObject, TraceStack traceStack) {
        this.delegate = delegate;
        this.linkedRule = linkedRule;
        this.condition = condition;
        this.baseTraceObject = baseTraceObject;
        this.traceStack = traceStack;
    }

    @SuppressWarnings("unchecked")
    @Override
    public int compareTo(T o) {
        if (o instanceof ComparableValueTraceDecorator) {
            o = (T) ((ComparableValueTraceDecorator) o).delegate;
        }
        int result = delegate.compareTo(o);
        traceComparisonResult(result == 0);
        return result;
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ComparableValueTraceDecorator) {
            obj = ((ComparableValueTraceDecorator) obj).delegate;
        }
        boolean result = delegate.equals(obj);
        traceComparisonResult(result);
        return result;
    }

    public void traceComparisonResult(boolean successful) {
        if (!linkedRule.getRulesIterator().hasNext()) {
            // Do not trace index value that is not mapped to any rule. This can
            // be an excluding boundary for example.
            return;
        }

        // TODO: remove side effect from the push method
        traceStack.push(new DTIndexedTraceObject(baseTraceObject, condition, linkedRule, successful));

        if (!successful) {
            traceStack.pop();
        }
    }

}
