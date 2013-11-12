package org.openl.rules.method;

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
     * Invoke for trace operation.
     */
    Object invokeTraced(Object target, Object[] params, IRuntimeEnv env);
    
    /**
     * Invoke for simple run operation. 
     */
    Object invokeSimple(Object target, Object[] params, IRuntimeEnv env);
       
}
