package org.openl.vm.trace;

/**
 * This trace stack after reset removes only child trace objects (trace objects
 * added to decorated tracer will not be removed)
 * 
 * @author NSamatov
 * 
 */
public class ChildTraceStack implements TraceStack {
    private final TraceStack tracer;
    private final int initialSize;

    public ChildTraceStack(TraceStack tracer) {
        this.tracer = tracer;
        initialSize = tracer.size();
    }

    @Override
    public void push(ITracerObject tracerObject) {
        tracer.push(tracerObject);
    }

    @Override
    public void pop() {
        tracer.pop();
    }

    @Override
    public void reset() {
        while (tracer.size() - initialSize > 0) {
            pop();
        }
    }

    @Override
    public int size() {
        return tracer.size();
    }

}