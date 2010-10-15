package org.openl.rules.table;

import org.openl.exception.OpenLRuntimeException;
import org.openl.types.Invokable;

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
     */
    void setErrorToTrace(OpenLRuntimeException error);
    
    /**
     * Creates traceable node for current invokable object.
     * 
     * @return {@link ATableTracerNode} for current invokable object.
     */
    ATableTracerNode createTraceObject();
    
    /**
     * Invoke for trace operation.
     */
    Object invokeTraced();
    
    /**
     * Invoke for simple run operation. 
     */
    Object invokeSimple();
    
    /**
     * Gets the error.
     * 
     * @return error.
     */
    OpenLRuntimeException getError();
}
