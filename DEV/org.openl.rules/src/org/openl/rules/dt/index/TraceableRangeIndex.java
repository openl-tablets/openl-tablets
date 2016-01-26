package org.openl.rules.dt.index;

import java.util.Iterator;

import org.openl.rules.dt.DecisionTableRuleNode;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dtx.trace.DTIndexedTraceObject;
import org.openl.rules.dtx.trace.DecisionTableTraceObject;
import org.openl.vm.trace.ITracerObject;
import org.openl.vm.trace.TraceStack;

public class TraceableRangeIndex extends RangeIndex {
    private final ICondition condition;
    private final DecisionTableTraceObject baseTraceObject;
    private CachingTraceStack cachingTraceStack;

    @SuppressWarnings({"rawtypes", "unchecked"})
    public TraceableRangeIndex(RangeIndex delegate, ICondition condition, DecisionTableTraceObject baseTraceObject,
                               TraceStack traceStack) {
        super(delegate.emptyOrFormulaNodes, null, delegate.rules, delegate.adaptor);
        this.condition = condition;
        this.baseTraceObject = baseTraceObject;

        index = new Comparable<?>[delegate.index.length];

        cachingTraceStack = new CachingTraceStack(traceStack);

        for (int i = 0; i < index.length; i++) {
            index[i] = new ComparableIndexTraceDecorator(delegate.index[i], rules[i], condition, baseTraceObject,
                    cachingTraceStack);
        }
    }

    @Override
    public DecisionTableRuleNode findNode(Object value) {
        cachingTraceStack.reset();

        DecisionTableRuleNode result = super.findNode(value != null ? new ComparableValueTraceDecorator(value) : null); 

        if (result == emptyOrFormulaNodes) {
            if (result.getRules() != null && result.getRules().length > 0) {
                cachingTraceStack.push(new DTIndexedTraceObject(baseTraceObject, condition, result, true));
            }
        } else {
            Iterator<ITracerObject> iterator = cachingTraceStack.getTraceObjects().iterator();
            while (iterator.hasNext()) {
                DTIndexedTraceObject traceObject = (DTIndexedTraceObject) iterator.next();
                if (traceObject.getLinkedRule() == result && !traceObject.isSuccessful()) {
                    // Parameter is not equal to range boundaries but inside
                    // them (it is linked to result and result != emptyOrFormulaNodes)
                    // We should change trace result to successful and move it to the stack's end
                    // because we found that a result a result after checking all boundaries.
                    iterator.remove();
                    traceObject.setSuccessful(true);
                    cachingTraceStack.push(traceObject);
                    break;
                }
            }
        }

        cachingTraceStack.commit();

        return result;
    }

    @Override
    protected Object convertValueForSearch(Object value) {
        if (value instanceof ComparableValueTraceDecorator) {
            value = ((ComparableValueTraceDecorator) value).delegate;
        }
        return new ComparableValueTraceDecorator(super.convertValueForSearch(value));
    }
}
