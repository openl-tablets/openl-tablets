package org.openl.binding.impl.method;

import org.openl.binding.MethodUtil;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;

public abstract class AOpenMethodDelegator implements IOpenMethod, IMethodSignature {
    private IOpenMethod delegate;

    public AOpenMethodDelegator(IOpenMethod delegate) {
        if (delegate == null) {
            throw new IllegalArgumentException();
        }
        this.delegate = delegate;
    }
    
    public IOpenMethod getDelegate() {
        return delegate;
    }

    public IOpenClass getType() {
        return getDelegate().getType();
    }

    public IOpenClass getDeclaringClass() {
        return getDelegate().getDeclaringClass();
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
        return getDelegate().getName();
    }

    public int getNumberOfParameters() {
        return getDelegate().getSignature().getNumberOfParameters();
    }

    public String getParameterName(int i) {
        return getDelegate().getSignature().getParameterName(i);
    }

    public IOpenClass getParameterType(int i) {
        return getDelegate().getSignature().getParameterType(i);
    }

    public IOpenClass[] getParameterTypes() {
        return getDelegate().getSignature().getParameterTypes();
    }

    public IMethodSignature getSignature() {
        return this;
    }

    public boolean isStatic() {
        return getDelegate().isStatic();
    }

    @Override
    public boolean isConstructor() {
        return false;
    }

    @Override
    public String toString() {
        return getName();
    }

}
