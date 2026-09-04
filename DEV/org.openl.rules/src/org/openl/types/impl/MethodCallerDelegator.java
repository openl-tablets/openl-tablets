package org.openl.types.impl;

import lombok.Getter;

import org.openl.types.IMethodCaller;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;

public class MethodCallerDelegator implements IMethodCaller {
    @Getter
    private final IMethodCaller delegate;

    public MethodCallerDelegator(IMethodCaller delegate) {
        this.delegate = delegate;
    }

    @Override
    public IOpenMethod getMethod() {
        return delegate.getMethod();
    }

    @Override
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        return delegate.invoke(target, params, env);
    }

}
