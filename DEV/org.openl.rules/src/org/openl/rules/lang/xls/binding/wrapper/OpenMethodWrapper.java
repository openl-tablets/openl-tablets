package org.openl.rules.lang.xls.binding.wrapper;

import java.util.Objects;

import org.openl.rules.lang.xls.binding.ModuleRelatedType;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;

public class OpenMethodWrapper implements IOpenMethod, IOpenMethodWrapper {
    private final IOpenMethod delegate;
    private final IOpenClass type;
    private final IMethodSignature methodSignature;
    private final IOpenClass declaringClass;

    public OpenMethodWrapper(XlsModuleOpenClass xlsModuleOpenClass, IOpenMethod method) {
        this.delegate = Objects.requireNonNull(method, "method cannot be null");
        if (delegate.getType() instanceof ModuleRelatedType) {
            IOpenClass type = xlsModuleOpenClass.findType(delegate.getType().getName());
            this.type = type != null ? type : delegate.getType();
        } else {
            this.type = delegate.getType();
        }
        this.methodSignature = WrapperLogic.buildMethodSignature(method, xlsModuleOpenClass);
        this.declaringClass = xlsModuleOpenClass;
    }

    public IOpenMethod getDelegate() {
        return delegate;
    }

    @Override
    public boolean isConstructor() {
        return delegate.isConstructor();
    }

    @Override
    public IOpenMethod getMethod() {
        return this;
    }

    @Override
    public IMethodSignature getSignature() {
        return methodSignature;
    }

    @Override
    public IOpenClass getType() {
        return type;
    }

    @Override
    public boolean isStatic() {
        return delegate.isStatic();
    }

    @Override
    public IMemberMetaInfo getInfo() {
        return delegate.getInfo();
    }

    @Override
    public IOpenClass getDeclaringClass() {
        return declaringClass;
    }

    @Override
    public String getDisplayName(int mode) {
        return delegate.getDisplayName(mode);
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        return delegate.invoke(target, params, env);
    }
}
