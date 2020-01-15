package org.openl.types.impl;

import java.lang.reflect.Constructor;
import java.util.Objects;

import org.openl.types.IMemberMetaInfo;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.IParameterDeclaration;
import org.openl.types.java.JavaOpenConstructor;
import org.openl.vm.IRuntimeEnv;

public class DatatypeOpenConstructor extends JavaOpenConstructor {

    private static final IParameterDeclaration[] EMPTY = new IParameterDeclaration[0];

    private final JavaOpenConstructor delegator;
    private final IOpenClass declaringClass;
    private final IParameterDeclaration[] parameters;

    public DatatypeOpenConstructor(JavaOpenConstructor delegator, IOpenClass declaringClass) {
        this(delegator, declaringClass, EMPTY);
    }

    public DatatypeOpenConstructor(JavaOpenConstructor delegator, IOpenClass declaringClass, IParameterDeclaration[] parameters) {
        super(delegator.getJavaConstructor());
        this.delegator = delegator;
        this.declaringClass = declaringClass;
        this.parameters = Objects.requireNonNull(parameters);
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
        return parameters.length;
    }

    @Override
    public String getParameterName(int i) {
        return parameters[i].getName();
    }

    @Override
    public IOpenClass getParameterType(int i) {
        return parameters[i].getType();
    }

    @Override
    public IOpenClass[] getParameterTypes() {
        if (parameters.length == 0) {
            return IOpenClass.EMPTY;
        }
        IOpenClass[] parameterTypes = new IOpenClass[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            parameterTypes[i] = parameters[i].getType();
        }
        return parameterTypes;
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
