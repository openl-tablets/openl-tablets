package org.openl.types.java;

import org.openl.binding.MethodUtil;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IMethodCaller;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.vm.IRuntimeEnv;

public class AutoCastResultOpenMethod implements IOpenMethod, IMethodSignature {
    private IMethodCaller methodCaller;
    
    private IOpenCast cast;

    private IOpenClass returnType;

    public AutoCastResultOpenMethod(IMethodCaller methodCaller, IOpenClass returnType, IOpenCast cast) {
        if (methodCaller == null){
            throw new IllegalArgumentException();
        }
        if (returnType == null){
            throw new IllegalArgumentException();
        }
        if (cast == null){
            throw new IllegalArgumentException();
        }
        this.methodCaller = methodCaller;
        this.returnType = returnType;
        this.cast = cast;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenMember#getDeclaringClass()
     */
    public IOpenClass getDeclaringClass() {
        return methodCaller.getMethod().getDeclaringClass();
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
        return methodCaller.getMethod().getName();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IMethodSignature#getNumberOfParameters()
     */
    public int getNumberOfParameters() {
        return methodCaller.getMethod().getSignature().getNumberOfParameters();
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IMethodSignature#getParameterName(int)
     */
    public String getParameterName(int i) {
        return methodCaller.getMethod().getSignature().getParameterName(i);
    }
    
    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IMethodSignature#getParameterType(int)
     */
    public IOpenClass getParameterType(int i) {
        return methodCaller.getMethod().getSignature().getParameterType(i);
    }

    public IOpenClass[] getParameterTypes() {
        return methodCaller.getMethod().getSignature().getParameterTypes();
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
        return returnType;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenMethod#invoke(java.lang.Object,
     *      java.lang.Object[])
     */
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        try {
            return cast.convert(methodCaller.invoke(target, params, env));
        } catch (Throwable t) {
            throw RuntimeExceptionWrapper.wrap(t);
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.types.IOpenMember#isStatic()
     */
    public boolean isStatic() {
        return methodCaller.getMethod().isStatic();
    }

    @Override
    public String toString() {
        return getName();
    }
}