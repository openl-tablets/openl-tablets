package org.openl.rules.table;

import org.openl.exception.OpenLRuntimeException;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.trace.Tracer;

/**
 * Default implementation for invokers supporting tracing.
 * 
 * @author DLiauchuk
 *
 */
public abstract class DefaultInvokerWithTrace implements InvokerWithTrace {
    
    public void setErrorToTrace(OpenLRuntimeException error, Object[] params) {
        Tracer tracer = Tracer.getTracer();    
        ATableTracerNode traceObject = createTraceObject(params);
        traceObject.setError(error);
        tracer.push(traceObject);
        tracer.pop();
    }
        
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        // check if the object can be invoked
        //
        if (canInvoke()) {
            if (isTracerOn()) {
                // invoke in trace
                return invokeTraced(target, params, env);
            } else {
                // simple run invoke
                return invokeSimple(target, params, env);
            }
        } else {
            // object can`t be invoked, inform user about the problem.
            OpenLRuntimeException error = getError();
            if (isTracerOn()) {
                setErrorToTrace(error, params);
            } 
            throw error;            
        }
    }
    
    protected boolean isTracerOn() {
        return Tracer.isTracerOn();
    }
}
