package org.openl.types.impl;

import java.lang.reflect.Constructor;

import org.openl.types.IMemberMetaInfo;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenConstructor;
import org.openl.vm.IRuntimeEnv;

public class DatatypeOpenConstructor extends JavaOpenConstructor {

    private final JavaOpenConstructor delegator;
    private final IOpenClass declaringClass;

    public DatatypeOpenConstructor(JavaOpenConstructor delegator, IOpenClass declaringClass) {
        super(delegator.getJavaConstructor());
        this.delegator = delegator;
        this.declaringClass = declaringClass;
    }

    @Override
    public IOpenClass getDeclaringClass() {
        return declaringClass;
    }

    @Override
    public String getDisplayName(int mode) {
        return delegator.getDisplayName(mode);
    }

    @Override
    public IMemberMetaInfo getInfo() {
        return delegator.getInfo();
    }

    @Override
    public IOpenMethod getMethod() {
        return this;
    }

    @Override
    public String getName() {
        return delegator.getName();
    }

    @Override
    public int getNumberOfParameters() {
        return delegator.getNumberOfParameters();
    }

    @Override
    public String getParameterName(int i) {
        return delegator.getParameterName(i);
    }

    @Override
    public IOpenClass getParameterType(int i) {
        return delegator.getParameterType(i);
    }

    @Override
    public IOpenClass[] getParameterTypes() {
        return delegator.getParameterTypes();
    }

    @Override
    public IMethodSignature getSignature() {
        return this;
    }

    @Override
    public IOpenClass getType() {
        return declaringClass;
    }

    @Override
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        return delegator.invoke(target, params, env);
    }

    @Override
    public boolean isStatic() {
        return delegator.isStatic();
    }

    @Override
    public boolean isConstructor() {
        return delegator.isConstructor();
    }

    @Override
    public String toString() {
        return getDeclaringClass().getName();
    }

    @Override
    public Constructor<?> getJavaConstructor() {
        return delegator.getJavaConstructor();
    }
}
