/**
 *
 */
package org.openl.rules.tbasic.runtime;

import lombok.Getter;

import org.openl.IOpenRunner;
import org.openl.rules.cloner.Cloner;
import org.openl.runtime.IRuntimeContext;
import org.openl.types.impl.DelegatedDynamicObject;
import org.openl.vm.CheckedAliasValues;
import org.openl.vm.IRuntimeEnv;
import org.openl.vm.Tracer;

/**
 * The <code>TBasicContextHolderEnv</code> contains full context for execution of Algorithm: runtime environment, VM,
 * variables and parameters to run with.
 */
public class TBasicContextHolderEnv implements IRuntimeEnv {
    @Getter
    private final IRuntimeEnv env;
    @Getter
    private final TBasicVM tbasicVm;
    @Getter
    private final DelegatedDynamicObject tbasicTarget;
    @Getter
    private final Object[] tbasicParams;

    /**
     * Create an instance of <code>TBasicContextHolderEnv</code> initialized with environment,
     * <code>DelegatedDynamicObject</code> for variables,execution VM and parameters.
     *
     * @param env
     * @param tbasicTarget
     * @param params
     * @param tbasicVM
     */
    public TBasicContextHolderEnv(IRuntimeEnv env,
                                  DelegatedDynamicObject tbasicTarget,
                                  Object[] params,
                                  TBasicVM tbasicVM) {
        this.env = env;
        tbasicVm = tbasicVM;
        tbasicParams = params;
        this.tbasicTarget = tbasicTarget;
    }

    /**
     * Create new variable in context(if variable with specified name does not exist) and sets its value.
     *
     * @param variableName Name of variable to initiate.
     * @param value        Initial value of new variable.
     */
    public void assignValueToVariable(String variableName, Object value) {
        tbasicTarget.setFieldValue(variableName, value);
    }

    @Override
    public Object[] getLocalFrame() {
        return env.getLocalFrame();
    }

    @Override
    public IOpenRunner getRunner() {
        return env.getRunner();
    }

    @Override
    public Object getThis() {
        return env.getThis();
    }

    @Override
    public Object[] popLocalFrame() {
        return env.popLocalFrame();
    }

    @Override
    public Object popThis() {
        return env.popThis();
    }

    @Override
    public void pushLocalFrame(Object[] frame) {
        env.pushLocalFrame(frame);
    }

    @Override
    public void pushThis(Object thisObject) {
        env.pushThis(thisObject);
    }

    @Override
    public IRuntimeContext getContext() {
        return env.getContext();
    }

    @Override
    public void setContext(IRuntimeContext context) {
        env.setContext(context);
    }

    @Override
    public boolean isContextManagingSupported() {
        return env.isContextManagingSupported();
    }

    @Override
    public IRuntimeContext popContext() {
        return env.popContext();
    }

    @Override
    public void pushContext(IRuntimeContext context) {
        env.pushContext(context);
    }

    @Override
    public IRuntimeEnv copy() {
        var copiedParams = tbasicParams;
        try {
            copiedParams = Cloner.clone(tbasicParams);
        } catch (Exception ignored) {
            // ignore
        }
        return new TBasicContextHolderEnv(env.copy(),
                tbasicTarget,
                copiedParams,
                tbasicVm
        );
    }

    @Override
    public Tracer getTracer() {
        return env.getTracer();
    }

    @Override
    public void setTracer(Tracer tracer) {
        env.setTracer(tracer);
    }

    @Override
    public CheckedAliasValues getCheckedAliasValues() {
        return env.getCheckedAliasValues();
    }
}
