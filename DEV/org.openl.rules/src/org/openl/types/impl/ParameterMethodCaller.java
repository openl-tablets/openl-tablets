package org.openl.types.impl;

import org.openl.types.Invokable;
import org.openl.vm.IRuntimeEnv;

/**
 * @author Yury Molchan
 */
public class ParameterMethodCaller implements Invokable {

    private final int parameterNumber;

    public ParameterMethodCaller(int parameterNumber) {
        this.parameterNumber = parameterNumber;
    }

    @Override
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        return params[parameterNumber];
    }
}
