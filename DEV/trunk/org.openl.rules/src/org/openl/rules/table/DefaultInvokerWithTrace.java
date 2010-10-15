package org.openl.rules.table;

import org.openl.exception.OpenLRuntimeException;
import org.openl.types.impl.Invoker;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.trace.Tracer;

/**
 * Default implementation for invokers supporting tracing.
 * 
 * @author DLiauchuk
 *
 */
public abstract class DefaultInvokerWithTrace extends Invoker implements InvokerWithTrace {
    
    public DefaultInvokerWithTrace(Object target, Object[] params, IRuntimeEnv env) {
        super(target, params, env);
    }
    
    public void setErrorToTrace(OpenLRuntimeException error) {
        Tracer tracer = Tracer.getTracer();    
        ATableTracerNode traceObject = createTraceObject();
        traceObject.setError(error);
        tracer.push(traceObject);
        tracer.pop();
    }
        
    public Object invoke() {
        // check if the object can be invoked
        //
        if (canInvoke()) {
            if (isTracerOn()) {
                // invoke in trace
                return invokeTraced();
            } else {
                // simple run invoke
                return invokeSimple();
            }
        } else {
            // object can`t be invoked, inform user about the problem.
            OpenLRuntimeException error = getError();
            if (isTracerOn()) {
                setErrorToTrace(error);
            } 
            throw error;            
        }
    }
    
    protected boolean isTracerOn() {
        return Tracer.isTracerOn();
    }
}
