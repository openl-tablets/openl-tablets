package org.openl.rules.method;

import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.table.ATableTracerNode;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.trace.Tracer;

/**
 * Default implementation for invokers supporting tracing.
 * 
 * @author DLiauchuk
 *
 */
public abstract class DefaultInvokerWithTrace implements InvokerWithTrace {
        
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
    
    /**
    * Gets the error.
    * 
    * @return error.
    */
    protected abstract OpenLRuntimeException getError();
    
    
    /**
     * Creates traceable node for current invokable object.
     * 
     * @param params
     * @return {@link ATableTracerNode} for current invokable object.
     */
    protected abstract ATableTracerNode getTraceObject(Object[] params);
    
    protected  boolean isTracerOn() {
        return Tracer.isTracerOn();
    }
    
    protected void setErrorToTrace(OpenLRuntimeException error, Object[] params) {
        Tracer tracer = Tracer.getTracer();
        ATableTracerNode traceObject = getTraceObject(params);
        traceObject.setError(error);
        tracer.push(traceObject);
        tracer.pop();
    }
}
