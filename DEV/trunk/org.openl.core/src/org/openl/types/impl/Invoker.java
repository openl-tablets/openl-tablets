package org.openl.types.impl;

import org.openl.types.Invokable;
import org.openl.vm.IRuntimeEnv;

/**
 * Default invoker.
 * 
 * @author DLiauchuk
 *
 */
public abstract class Invoker implements Invokable {

    private Object target;
    private Object[] params;
    private IRuntimeEnv env;
    
    public Invoker(Object target, Object[] params, IRuntimeEnv env) {
        this.target = target;
        this.params = params;
        this.env = env;
    }
    
    public abstract Object invoke();
    
    protected Object[] getParams() {
        return params;
    }

    protected Object getTarget() {
        return target;
    }

    protected IRuntimeEnv getEnv() {
        return env;
    }
    
    /**
     * Reset previously initialized parameters with new ones.
     * 
     * @param target
     * @param params
     * @param env
     */
    public void resetParams(Object target, Object[] params, IRuntimeEnv env) {
        clean();
        this.target = target;
        this.params = params;
        this.env = env;
        
    }
    
    private void clean() {
        this.target = null;
        this.params = null;
        this.env = null;
    }

}
