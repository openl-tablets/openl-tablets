package org.openl.types.impl;

import java.lang.reflect.Method;

import org.openl.binding.MethodUtil;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenMethod;
import org.openl.vm.IRuntimeEnv;

public class DatatypeOpenMethod extends JavaOpenMethod {
    JavaOpenMethod method;
    IOpenClass[] parameterTypes;
    IOpenClass declaringClass;
    IOpenClass type;

    public DatatypeOpenMethod(JavaOpenMethod method,
            IOpenClass declaringClass,
            IOpenClass[] parameterTypes,
            IOpenClass type) {
        super(method.getJavaMethod());
        this.method = method;
        this.parameterTypes = parameterTypes;
        this.declaringClass = declaringClass;
        this.type = type;
    }

    @Override
    public IOpenClass getDeclaringClass() {
        return declaringClass;
    }

    @Override
    public String getDisplayName(int mode) {
        return MethodUtil.printSignature(this, mode);
    }

    @Override
    public IMemberMetaInfo getInfo() {
        return null;
    }

    @Override
    public IOpenMethod getMethod() {
        return this;
    }

    @Override
    public String getName() {
        return method.getName();
    }

    @Override
    public int getNumberOfParameters() {
        return getParameterTypes().length;
    }

    @Override
    public String getParameterName(int i) {
        return null;
    }

    @Override
    public IOpenClass getParameterType(int i) {
        return getParameterTypes()[i];
    }

    @Override
    public IOpenClass[] getParameterTypes() {
        return parameterTypes;
    }

    @Override
    public IMethodSignature getSignature() {
        return this;
    }

    @Override
    public IOpenClass getType() {
        return type;
    }

    @Override
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        return method.invoke(target, params, env);
    }

    @Override
    public boolean isStatic() {
        return method.isStatic();
    }

    @Override
    public Method getJavaMethod() {
        return method.getJavaMethod();
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public boolean isConstructor() {
        return method.isConstructor();
    }
}
