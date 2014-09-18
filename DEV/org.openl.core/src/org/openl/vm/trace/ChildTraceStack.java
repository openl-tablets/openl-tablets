package org.openl.vm.trace;

/**
 * This trace stack after reset removes only child trace objects (trace objects
 * added to decorated tracer will not be removed)
 * 
 * @author NSamatov
 * 
 */
public class ChildTraceStack implements TraceStack {
    private int pushed = 0;

    @Override
    public void push(ITracerObject tracerObject) {
        Tracer.begin(tracerObject);
        pushed++;
    }

    @Override
    public void pop() {
        Tracer.end();
        pushed--;
    }

    @Override
    public void reset() {
        while (pushed > 0) {
            pop();
        }
    }
}
