package org.openl.rules.dt.index;

import java.util.Comparator;
import java.util.HashMap;
import java.util.SortedMap;
import java.util.TreeMap;

import org.openl.rules.dt.DecisionTableRuleNode;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dtx.trace.DTIndexedTraceObject;
import org.openl.rules.dtx.trace.DecisionTableTraceObject;
import org.openl.vm.trace.TraceStack;

public class TraceableEqualsIndex extends EqualsIndex {
    private final ICondition condition;
    private final DecisionTableTraceObject baseTraceObject;
    private final TraceStack traceStack;

    public TraceableEqualsIndex(EqualsIndex delegate,
                                ICondition condition,
            DecisionTableTraceObject baseTraceObject,
                                TraceStack traceStack) {
        super(delegate.emptyOrFormulaNodes);
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
                        Object p1 = unwrap(o1);
                        Object p2 = unwrap(o2);
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

    @SuppressWarnings("unchecked")
    protected Object unwrap(Object value) {
        if (value instanceof ComparableValueTraceDecorator) {
            return ((ComparableValueTraceDecorator) value).delegate;
        } else if (value instanceof ComparableIndexTraceDecorator) {
            return ((ComparableIndexTraceDecorator<Object>) value).delegate;
        }

        return value;
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
