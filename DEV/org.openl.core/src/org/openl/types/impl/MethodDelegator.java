package org.openl.types.impl;

import org.openl.types.*;
import org.openl.vm.IRuntimeEnv;

public class MethodDelegator implements IOpenMethod {

    protected IMethodCaller methodCaller;

    public MethodDelegator(IMethodCaller methodCaller) {
        this.methodCaller = methodCaller;
    }

    @Override
    public IMethodSignature getSignature() {
        return methodCaller.getMethod().getSignature();
    }

    @Override
    public IOpenClass getDeclaringClass() {
        return methodCaller.getMethod().getDeclaringClass();
    }

    @Override
    public IMemberMetaInfo getInfo() {
        return methodCaller.getMethod().getInfo();
    }

    @Override
    public IOpenClass getType() {
        return methodCaller.getMethod().getType();
    }

    @Override
    public boolean isStatic() {
        return methodCaller.getMethod().isStatic();
    }

    @Override
    public String getDisplayName(int mode) {
        return methodCaller.getMethod().getDisplayName(mode);
    }

    @Override
    public String getName() {
        return methodCaller.getMethod().getName();
    }

    @Override
    public IOpenMethod getMethod() {
        return methodCaller.getMethod();
    }

    @Override
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        return methodCaller.invoke(target, params, env);
    }

    @Override
    public boolean isConstructor() {
        return methodCaller.getMethod().isConstructor();
    }
}
