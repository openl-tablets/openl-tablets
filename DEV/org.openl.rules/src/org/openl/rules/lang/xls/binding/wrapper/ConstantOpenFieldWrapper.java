package org.openl.rules.lang.xls.binding.wrapper;

import org.openl.rules.constants.ConstantOpenField;
import org.openl.rules.lang.xls.binding.wrapper.base.WrapperValidation;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

public class ConstantOpenFieldWrapper extends ConstantOpenField {
    static {
        WrapperValidation.validateWrapperClass(ConstantOpenFieldWrapper.class,
            ConstantOpenFieldWrapper.class.getSuperclass());
    }

    private final ConstantOpenField delegate;
    private final IOpenClass type;

    public ConstantOpenFieldWrapper(ConstantOpenField delegate, IOpenClass type) {
        this.delegate = delegate;
        this.type = type;
    }

    public ConstantOpenField getDelegate() {
        return delegate;
    }

    @Override
    public boolean isContextProperty() {
        return delegate.isContextProperty();
    }

    @Override
    public String getContextProperty() {
        return delegate.getContextProperty();
    }

    @Override
    public boolean isTransient() {
        return delegate.isTransient();
    }

    @Override
    public String getDisplayName(int mode) {
        return delegate.getDisplayName(mode);
    }

    @Override
    public IMemberMetaInfo getInfo() {
        return delegate.getInfo();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public IOpenClass getType() {
        return type;
    }

    @Override
    public boolean isReadable() {
        return delegate.isReadable();
    }

    @Override
    public boolean isStatic() {
        return delegate.isStatic();
    }

    @Override
    public void setType(IOpenClass class1) {
        delegate.setType(class1);
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @Override
    public String getValueAsString() {
        return delegate.getValueAsString();
    }

    @Override
    public IOpenClass getDeclaringClass() {
        return delegate.getDeclaringClass();
    }

    @Override
    public Object get(Object target, IRuntimeEnv env) {
        return delegate.get(target, env);
    }

    @Override
    public Object getValue() {
        return delegate.getValue();
    }

    @Override
    public boolean isWritable() {
        return delegate.isWritable();
    }

    @Override
    public IMemberMetaInfo getMemberMetaInfo() {
        return delegate.getMemberMetaInfo();
    }

    @Override
    public void setMemberMetaInfo(IMemberMetaInfo memberMetaInfo) {
        delegate.setMemberMetaInfo(memberMetaInfo);
    }

    @Override
    public boolean isConst() {
        return delegate.isConst();
    }

    @Override
    public void set(Object target, Object value, IRuntimeEnv env) {
        delegate.set(target, value, env);
    }
}
