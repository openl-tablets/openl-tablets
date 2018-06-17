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

    IOpenClass[] parameterTypes;

    public JavaOpenMethod(Method method) {
        this.method = method;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenMember#getDeclaringClass()
     */
    public IOpenClass getDeclaringClass() {
        return JavaOpenClass.getOpenClass(method.getDeclaringClass());
    }

    public String getDisplayName(int mode) {
        return MethodUtil.printSignature(this, mode);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenMember#getInfo()
     */
    public IMemberMetaInfo getInfo() {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IMethodCaller#getMethod()
     */
    public IOpenMethod getMethod() {
        return this;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.base.INamedThing#getName()
     */
    public String getName() {
        return method.getName();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IMethodSignature#getNumberOfParameters()
     */
    public int getNumberOfParameters() {
        return getParameterTypes().length;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IMethodSignature#getParameterName(int)
     */
    public String getParameterName(int i) {
        return null;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IMethodSignature#getParameterType(int)
     */
    public IOpenClass getParameterType(int i) {
        return getParameterTypes()[i];
    }

    public IOpenClass[] getParameterTypes() {
        if (parameterTypes == null) {
            synchronized (this) {
                parameterTypes = JavaOpenClass.getOpenClasses(method.getParameterTypes());
            }

        }

        return parameterTypes;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenMethodHeader#getSignature()
     */
    public IMethodSignature getSignature() {
        return this;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenMember#getType()
     */
    public IOpenClass getType() {
        return JavaOpenClass.getOpenClass(method.getReturnType());
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenMethod#invoke(java.lang.Object,
     * java.lang.Object[])
     */
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        try {
            return method.invoke(target, params);
        } catch (InvocationTargetException t) {
            String msg = "Failure in the method: " + method + " on the target: " + String
                .valueOf(target) + " with values: [" + StringUtils.join(params, "], [") + "]. Cause: " + t.getTargetException().getMessage();
            throw new OpenLRuntimeException(msg, t);
        } catch (Exception t) {
            String msg = "Failure in the method: " + method + " on the target: " + String
                .valueOf(target) + " with values: [" + StringUtils.join(params, "], [") + "]. Cause: " + t.getMessage();
            throw new OpenLRuntimeException(msg, t);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenMember#isStatic()
     */
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
