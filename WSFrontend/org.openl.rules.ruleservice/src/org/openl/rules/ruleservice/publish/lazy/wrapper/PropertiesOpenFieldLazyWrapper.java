package org.openl.rules.ruleservice.publish.lazy.wrapper;

import java.util.Objects;

import org.openl.rules.lang.xls.binding.wrapper.base.WrapperValidation;
import org.openl.rules.property.PropertiesOpenField;
import org.openl.rules.ruleservice.publish.lazy.LazyMember;
import org.openl.rules.table.properties.TableProperties;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.vm.IRuntimeEnv;

public final class PropertiesOpenFieldLazyWrapper extends PropertiesOpenField {
    static {
        WrapperValidation.validateWrapperClass(PropertiesOpenFieldLazyWrapper.class,
            PropertiesOpenFieldLazyWrapper.class.getSuperclass());
    }

    private final PropertiesOpenField delegate;
    private final LazyMember<IOpenField> lazyField;

    PropertiesOpenFieldLazyWrapper(LazyMember<IOpenField> lazyField, PropertiesOpenField delegate) {
        super(null, delegate.getPropertiesInstance(), null);
        this.delegate = Objects.requireNonNull(delegate, "delegate cannot be null");
        this.lazyField = Objects.requireNonNull(lazyField, "lazyField cannot be null");
    }

    @Override
    public IOpenClass getDeclaringClass() {
        return delegate.getDeclaringClass();
    }

    @Override
    public Object get(Object target, IRuntimeEnv env) {
        return lazyField.getMember().get(target, env);
    }

    @Override
    public boolean isWritable() {
        return delegate.isWritable();
    }

    @Override
    public void set(Object target, Object value, IRuntimeEnv env) {
        delegate.set(target, value, env);
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
        return delegate.getType();
    }

    @Override
    public boolean isConst() {
        return delegate.isConst();
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
    public boolean isContextProperty() {
        return delegate.isContextProperty();
    }

    @Override
    public String getContextProperty() {
        return delegate.getContextProperty();
    }

    @Override
    public TableProperties getPropertiesInstance() {
        return delegate.getPropertiesInstance();
    }
}