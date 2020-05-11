package org.openl.rules.lang.xls.types;

import java.util.Objects;

import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.vm.IRuntimeEnv;

public class AliasDatatypeOpenField implements IOpenField {
    private final IOpenField delegate;
    private final String name;

    public AliasDatatypeOpenField(String name, IOpenField delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate cannot be null");
        this.name = Objects.requireNonNull(name, "name cannot be null");
    }

    @Override
    public Object get(Object target, IRuntimeEnv env) {
        return delegate.get(target, env);
    }

    @Override
    public void set(Object target, Object value, IRuntimeEnv env) {
        delegate.set(target, value, env);
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
    public boolean isContextProperty() {
        return delegate.isContextProperty();
    }

    @Override
    public String getContextProperty() {
        return delegate.getContextProperty();
    }

    @Override
    public boolean isWritable() {
        return delegate.isWritable();
    }

    @Override
    public IOpenClass getType() {
        return delegate.getType();
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
        return delegate.getDeclaringClass();
    }

    @Override
    public String getDisplayName(int mode) {
        return name;
    }

    @Override
    public String getName() {
        return name;
    }
}
