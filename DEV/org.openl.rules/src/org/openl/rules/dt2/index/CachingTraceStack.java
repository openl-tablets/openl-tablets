package org.openl.rules.dt2.index;

import org.openl.rules.dt2.trace.DTConditionTraceObject;
import org.openl.vm.trace.ITracerObject;
import org.openl.vm.trace.TraceStack;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by ymolchan on 9/19/2014.
 */
class CachingTraceStack implements TraceStack {
    private final TraceStack delegate;
    private LinkedList<ITracerObject> stack = new LinkedList<ITracerObject>();

    CachingTraceStack(TraceStack delegate) {
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

    List<ITracerObject> getTraceObjects() {
        return stack;
    }

    void commit() {
        for (int i = 0; i < stack.size(); i++) {
            ITracerObject t = stack.get(i);
            delegate.push(t);
            if (!((DTConditionTraceObject) t).isSuccessful()) {
                delegate.pop();
            }
        }
    }
}
