package org.openl.rules.dt.index;

import org.openl.rules.dt.DecisionTableRuleNode;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dtx.trace.DTIndexedTraceObject;
import org.openl.rules.dtx.trace.DecisionTableTraceObject;
import org.openl.vm.trace.TraceStack;

public class ComparableIndexTraceDecorator<T> implements Comparable<T> {
    protected final Comparable<T> delegate;
    protected final DecisionTableRuleNode linkedRule;
    protected final ICondition condition;
    protected final DecisionTableTraceObject baseTraceObject;
    protected final TraceStack traceStack;

    public ComparableIndexTraceDecorator(Comparable<T> delegate, DecisionTableRuleNode linkedRule,
            ICondition condition, DecisionTableTraceObject baseTraceObject, TraceStack traceStack) {
        this.delegate = delegate;
        this.linkedRule = linkedRule;
        this.condition = condition;
        this.baseTraceObject = baseTraceObject;
        this.traceStack = traceStack;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public int compareTo(T o) {
        boolean tracableCompare = false;
        if (o instanceof ComparableValueTraceDecorator) {
            o = (T) ((ComparableValueTraceDecorator) o).delegate;
            tracableCompare = true;
        }
        if (o instanceof ComparableIndexTraceDecorator) {
            o = (T) ((ComparableIndexTraceDecorator) o).delegate;
        }
        int result = delegate.compareTo(o);
        if (tracableCompare){
            traceComparisonResult(result == 0);
        }
        return result;
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public boolean equals(Object obj) {
        boolean tracableEquals = false;
        if (obj instanceof ComparableValueTraceDecorator) {
            obj = ((ComparableValueTraceDecorator) obj).delegate;
            tracableEquals = true;
        }
        if (obj instanceof ComparableIndexTraceDecorator) {
            obj = (T) ((ComparableIndexTraceDecorator) obj).delegate;
        }
        boolean result = delegate.equals(obj);
        if (tracableEquals){
            traceComparisonResult(result);
        }
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
