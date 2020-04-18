package org.openl.rules.ruleservice.publish.lazy.wrapper;

import java.util.Objects;

import org.openl.rules.data.DataOpenField;
import org.openl.rules.data.ITable;
import org.openl.rules.lang.xls.binding.wrapper.base.WrapperValidation;
import org.openl.rules.ruleservice.publish.lazy.LazyField;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

public final class DataOpenFieldLazyWrapper extends DataOpenField {
    static {
        WrapperValidation.validateWrapperClass(DataOpenFieldLazyWrapper.class,
            DataOpenFieldLazyWrapper.class.getSuperclass());
    }

    private final DataOpenField delegate;
    private final LazyField lazyField;

    public DataOpenFieldLazyWrapper(LazyField lazyField, DataOpenField delegate) {
        super();
        this.delegate = Objects.requireNonNull(delegate, "delegate cannot be null");
        this.lazyField = Objects.requireNonNull(lazyField, "lazyField cannot be null");
    }

    @Override
    public String getUri() {
        return delegate.getUri();
    }

    @Override
    public IOpenClass getDeclaringClass() {
        return delegate.getDeclaringClass();
    }

    @Override
    public ITable getTable() {
        return delegate.getTable();
    }

    @Override
    public Object getData() {
        return delegate.getData();
    }

    @Override
    public void setTable(ITable table) {
        delegate.setTable(table);
    }

    @Override
    public boolean isWritable() {
        return delegate.isWritable();
    }

    @Override
    public Object get(Object target, IRuntimeEnv env) {
        return lazyField.getMember().get(target, env);
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
}
