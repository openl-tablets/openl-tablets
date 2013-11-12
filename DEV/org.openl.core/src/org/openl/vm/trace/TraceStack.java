package org.openl.vm.trace;

public interface TraceStack {

    // TODO return popped value
    void pop();

    void push(ITracerObject obj);

    void reset();
    
    int size();

}