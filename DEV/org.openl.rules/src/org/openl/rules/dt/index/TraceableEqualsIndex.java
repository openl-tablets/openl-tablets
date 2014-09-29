package org.openl.rules.dt.index;

import org.openl.rules.dt.DecisionTableRuleNode;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.trace.DTIndexedTraceObject;
import org.openl.rules.dt.trace.DecisionTableTraceObject;
import org.openl.vm.trace.TraceStack;

import java.util.Comparator;
import java.util.HashMap;
import java.util.SortedMap;
import java.util.TreeMap;

public class TraceableEqualsIndex extends EqualsIndex {
    private final ICondition condition;
    private final DecisionTableTraceObject baseTraceObject;
    private final TraceStack traceStack;

    public TraceableEqualsIndex(EqualsIndex delegate,
                                ICondition condition,
                                DecisionTableTraceObject baseTraceObject,
                                TraceStack traceStack) {
        super(delegate.emptyOrFormulaNodes, null);
        this.condition = condition;
        this.baseTraceObject = baseTraceObject;
        this.traceStack = traceStack;

        if (delegate.valueNodes instanceof SortedMap) {
            final SortedMap<Object, ?> sortedMap = (SortedMap<Object, ?>) delegate.valueNodes;
            if (sortedMap.comparator() == null) {
                valueNodes = new TreeMap<Object, DecisionTableRuleNode>();
            } else {
                valueNodes = new TreeMap<Object, DecisionTableRuleNode>(new Comparator<Object>() {
                    @Override
                    public int compare(Object o1, Object o2) {
                        Object p1 = ((ComparableIndexTraceDecorator<?>) o1).delegate;
                        Object p2 = ((ComparableIndexTraceDecorator<?>) o2).delegate;
                        int result = sortedMap.comparator().compare(p1, p2);
                        return result;
                    }
                });
            }
        } else {
            valueNodes = new HashMap<Object, DecisionTableRuleNode>(delegate.valueNodes.size());
        }

        for (Object key : delegate.valueNodes.keySet()) {
            DecisionTableRuleNode linkedRule = delegate.valueNodes.get(key);

            @SuppressWarnings({"unchecked"})
            Comparable<?> newKey = new ComparableIndexTraceDecorator<Object>((Comparable<Object>) key,
                    linkedRule,
                    condition,
                    baseTraceObject,
                    traceStack);

            valueNodes.put(newKey, linkedRule);
        }
    }

    @Override
    public DecisionTableRuleNode findNode(Object value) {
        DecisionTableRuleNode result = super.findNode(value != null ? new ComparableValueTraceDecorator(value) : null);

        if (result == emptyOrFormulaNodes && result.getRules() != null && result.getRules().length > 0) {
            traceStack.push(new DTIndexedTraceObject(baseTraceObject, condition, result, true));
        }

        return result;
    }
}
