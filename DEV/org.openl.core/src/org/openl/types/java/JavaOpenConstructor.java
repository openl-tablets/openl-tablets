/*
 * Created on Jun 17, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.java;

import java.lang.reflect.Constructor;

import org.openl.binding.MethodUtil;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 *
 */
public class JavaOpenConstructor implements IOpenMethod, IMethodSignature {

    private Constructor<?> constructor;

    private IOpenClass[] parameterTypes;

    public JavaOpenConstructor(Constructor<?> constructor) {
        this.constructor = constructor;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenMember#getDeclaringClass()
     */
    @Override
    public IOpenClass getDeclaringClass() {
        return JavaOpenClass.getOpenClass(constructor.getDeclaringClass());
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
        return "<init>";
    }

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
        return "p" + i;
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
    public synchronized IOpenClass[] getParameterTypes() {
        if (parameterTypes == null) {
            parameterTypes = JavaOpenClass.getOpenClasses(constructor.getParameterTypes());
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
        return getDeclaringClass();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IMethodCaller#invoke(java.lang.Object, java.lang.Object[])
     */
    @Override
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        try {
            return constructor.newInstance(params);
        } catch (Exception t) {
            throw RuntimeExceptionWrapper.wrap(t);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenMember#isStatic()
     */
    @Override
    public boolean isStatic() {
        return true;
    }

    @Override
    public boolean isConstructor() {
        return true;
    }

    @Override
    public String toString() {
        return getDeclaringClass().getName();
    }

    public Constructor<?> getJavaConstructor() {
        return constructor;
    }
}
