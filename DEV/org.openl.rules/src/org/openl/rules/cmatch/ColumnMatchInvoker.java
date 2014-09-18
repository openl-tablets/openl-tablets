package org.openl.rules.cmatch;

import org.openl.rules.method.RulesMethodInvoker;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

public class ColumnMatchInvoker extends RulesMethodInvoker {

    public ColumnMatchInvoker(ColumnMatch columnMatch) {
        super(columnMatch);
    }

    @Override
    public ColumnMatch getInvokableMethod() {
        return (ColumnMatch) super.getInvokableMethod();
    }

    public boolean canInvoke() {
        return getInvokableMethod().getAlgorithmExecutor() != null;
    }

    public Object invokeSimple(Object target, Object[] params, IRuntimeEnv env) {
        Object result = getInvokableMethod().getAlgorithmExecutor().invoke(target, params, env, getInvokableMethod());

        if (result == null) {
            IOpenClass type = getInvokableMethod().getHeader().getType();
            if (type.getInstanceClass().isPrimitive()) {
                throw new IllegalArgumentException("Cannot return <null> for primitive type " + type.getInstanceClass()
                        .getName());
            }
        }

        return result;
    }

    /**
     * Column match is traceable. But trace is handled inside algorithm executor. See implementations
     * of {@link org.openl.rules.cmatch.algorithm.IMatchAlgorithmExecutor#invoke(Object target, Object[] params, IRuntimeEnv env, ColumnMatch columnMatch)}
     */
    public Object invokeTraced(Object target, Object[] params, IRuntimeEnv env) {
        // trace operations implemented in 
        // IMatchAlgorithmExecutor#invoke(Object target, Object[] params, IRuntimeEnv env, ColumnMatch columnMatch)}
        //
        return invokeSimple(target, params, env);
    }
}
