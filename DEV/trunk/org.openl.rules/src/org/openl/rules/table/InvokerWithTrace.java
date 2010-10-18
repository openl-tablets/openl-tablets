package org.openl.rules.table;

import org.openl.exception.OpenLRuntimeException;
import org.openl.types.Invokable;
import org.openl.vm.IRuntimeEnv;

/**
 * Invoker, supporting invokes for tracing.
 * 
 * @author DLiauchuk
 *
 */
public interface InvokerWithTrace extends Invokable {
    
    /** 
     * Checks if it is possible to invoke invokable object.
     */
    boolean canInvoke();
    
    /**
     * Sets the given error to the tracer.
     * 
     * @param error for setting to tracer. 
     * @param params
     */
    void setErrorToTrace(OpenLRuntimeException error, Object[] params);
    
    /**
     * Creates traceable node for current invokable object.
     * 
     * @param params
     * @return {@link ATableTracerNode} for current invokable object.
     */
    ATableTracerNode createTraceObject(Object[] params);
    
    /**
     * Invoke for trace operation.
     */
    Object invokeTraced(Object target, Object[] params, IRuntimeEnv env);
    
    /**
     * Invoke for simple run operation. 
     */
    Object invokeSimple(Object target, Object[] params, IRuntimeEnv env);
    
    /**
     * Gets the error.
     * 
     * @return error.
     */
    OpenLRuntimeException getError();
}
