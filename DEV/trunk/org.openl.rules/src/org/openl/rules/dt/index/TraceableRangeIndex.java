package org.openl.rules.dt.index;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.openl.rules.dt.DecisionTableRuleNode;
import org.openl.rules.dt.element.ICondition;
import org.openl.rules.dt.trace.DTConditionTraceObject;
import org.openl.rules.dt.trace.DTIndexedTraceObject;
import org.openl.rules.dt.trace.DecisionTableTraceObject;
import org.openl.vm.trace.ITracerObject;
import org.openl.vm.trace.TraceStack;

public class TraceableRangeIndex extends RangeIndex {
    private final ICondition condition;
    private final DecisionTableTraceObject baseTraceObject;
    private CachingTraceStack cachingTraceStack;

    @SuppressWarnings({ "rawtypes", "unchecked" })
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

    private static class CachingTraceStack implements TraceStack {
        private final TraceStack delegate;
        private List<ITracerObject> stack = new ArrayList<ITracerObject>();

        public CachingTraceStack(TraceStack delegate) {
            this.delegate = delegate;
        }

        @Override
        public void pop() {
        }

        @Override
        public void push(ITracerObject obj) {
            stack.add(obj);
        }

        @Override
        public void reset() {
            stack.clear();
        }

        public List<ITracerObject> getTraceObjects() {
            return stack;
        }

        public void commit() {
            for (int i = 0; i < stack.size(); i++) {
                ITracerObject t = stack.get(i);
                delegate.push(t);
                if (!((DTConditionTraceObject) t).isSuccessful()) {
                    delegate.pop();
                }
            }
        }
        
        @Override
        public int size() {
            return delegate.size() + stack.size();
        }


    }
}
