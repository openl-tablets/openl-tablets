/**
 * 
 */
package org.openl.rules.tbasic.runtime;

import org.openl.IOpenRunner;
import org.openl.types.impl.DelegatedDynamicObject;
import org.openl.vm.IRuntimeEnv;

/**
 * The <code>TBasicContextHolderEnv</code> contains full context for execution
 * of Algorithm: runtime environment, VM, variables and parameters to run with.
 * 
 */
public class TBasicContextHolderEnv implements IRuntimeEnv {
    private IRuntimeEnv env;
    private TBasicVM tbasicVm;
    private DelegatedDynamicObject tbasicTarget;
    private Object[] tbasicParams;

    public IRuntimeEnv getEnv() {
        return env;
    }

    public TBasicVM getTbasicVm() {
        return tbasicVm;
    }

    public DelegatedDynamicObject getTbasicTarget() {
        return tbasicTarget;
    }

    /**
     * @return the tbasicParams
     */
    public Object[] getTbasicParams() {
        return tbasicParams;
    }

    /**
     * Create an instance of <code>TBasicContextHolderEnv</code> initialized
     * with environment, <code>DelegatedDynamicObject</code> for
     * variables,execution VM and parameters.
     * 
     * @param env
     * @param tbasicTarget
     * @param params
     * @param tbasicVM
     */
    public TBasicContextHolderEnv(IRuntimeEnv env, DelegatedDynamicObject tbasicTarget, Object[] params,
            TBasicVM tbasicVM) {
        super();
        this.env = env;
        this.tbasicVm = tbasicVM;
        this.tbasicParams = params;
        this.tbasicTarget = tbasicTarget;
    }

    /**
     * Create new variable in context(if variable with specified name doesn't
     * exist) and sets its value.
     * 
     * @param variableName Name of variable to initiate.
     * @param value Initial value of new variable.
     */
    public void assignValueToVariable(String variableName, Object value) {
        tbasicTarget.setFieldValue(variableName, value);
    }

    public Object[] getLocalFrame() {
        return env.getLocalFrame();
    }

    public IOpenRunner getRunner() {
        return env.getRunner();
    }

    public Object getThis() {
        return env.getThis();
    }

    public Object[] popLocalFrame() {
        return env.popLocalFrame();
    }

    public Object popThis() {
        return env.popThis();
    }

    public void pushLocalFrame(Object[] frame) {
        env.pushLocalFrame(frame);
    }

    public void pushThis(Object thisObject) {
        env.pushThis(thisObject);
    }
}