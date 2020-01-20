/*
 * Created on May 20, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.java;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.openl.binding.MethodUtil;
import org.openl.exception.OpenLRuntimeException;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.util.StringUtils;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class JavaOpenMethod implements IOpenMethod, IMethodSignature {
    Method method;

    volatile IOpenClass[] parameterTypes;

    public JavaOpenMethod(Method method) {
        this.method = method;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenMember#getDeclaringClass()
     */
    @Override
    public IOpenClass getDeclaringClass() {
        return JavaOpenClass.getOpenClass(method.getDeclaringClass());
    }

    @Override
    public String getDisplayName(int mode) {
        return MethodUtil.printSignature(this, mode);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenMember#getInfo()
     */
    @Override
    public IMemberMetaInfo getInfo() {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IMethodCaller#getMethod()
     */
    @Override
    public IOpenMethod getMethod() {
        return this;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.base.INamedThing#getName()
     */
    @Override
    public String getName() {
        return method.getName();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IMethodSignature#getNumberOfParameters()
     */
    @Override
    public int getNumberOfParameters() {
        return getParameterTypes().length;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IMethodSignature#getParameterName(int)
     */
    @Override
    public String getParameterName(int i) {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IMethodSignature#getParameterType(int)
     */
    @Override
    public IOpenClass getParameterType(int i) {
        return getParameterTypes()[i];
    }

    @Override
    public IOpenClass[] getParameterTypes() {
        if (parameterTypes == null) {
            synchronized (this) {
                if (parameterTypes == null) {
                    parameterTypes = JavaOpenClass.getOpenClasses(method.getParameterTypes());
                }
            }

        }

        return parameterTypes;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenMethodHeader#getSignature()
     */
    @Override
    public IMethodSignature getSignature() {
        return this;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenMember#getType()
     */
    @Override
    public IOpenClass getType() {
        return JavaOpenClass.getOpenClass(method.getReturnType());
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenMethod#invoke(java.lang.Object, java.lang.Object[])
     */
    @Override
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        try {
            return method.invoke(target, params);
        } catch (InvocationTargetException t) {
            String msg = "Failure in the method: " + method + " on the target: " + target + " with values: [" + StringUtils
                .join(params, "], [") + "]. Cause: " + t.getTargetException().getMessage();
            throw new OpenLRuntimeException(msg, t);
        } catch (Exception t) {
            String msg = "Failure in the method: " + method + " on the target: " + target + " with values: [" + StringUtils
                .join(params, "], [") + "]. Cause: " + t.getMessage();
            throw new OpenLRuntimeException(msg, t);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenMember#isStatic()
     */
    @Override
    public boolean isStatic() {
        return Modifier.isStatic(method.getModifiers());
    }

    public Method getJavaMethod() {
        return method;
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public boolean isConstructor() {
        return false;
    }
}
