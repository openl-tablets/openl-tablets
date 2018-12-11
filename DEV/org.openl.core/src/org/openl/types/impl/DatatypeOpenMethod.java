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

    public IOpenClass getDeclaringClass() {
        return declaringClass;
    }

    public String getDisplayName(int mode) {
        return MethodUtil.printSignature(this, mode);
    }

    public IMemberMetaInfo getInfo() {
        return null;
    }

    public IOpenMethod getMethod() {
        return this;
    }

    public String getName() {
        return method.getName();
    }

    public int getNumberOfParameters() {
        return getParameterTypes().length;
    }

    public String getParameterName(int i) {
        return null;
    }

    public IOpenClass getParameterType(int i) {
        return getParameterTypes()[i];
    }

    public IOpenClass[] getParameterTypes() {
        return parameterTypes;
    }

    public IMethodSignature getSignature() {
        return this;
    }

    public IOpenClass getType() {
        return type;
    }

    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        return method.invoke(target, params, env);
    }

    public boolean isStatic() {
        return method.isStatic();
    }

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
