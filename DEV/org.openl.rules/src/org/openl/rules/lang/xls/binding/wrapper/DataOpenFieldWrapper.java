package org.openl.rules.lang.xls.binding.wrapper;

import org.openl.rules.data.DataOpenField;
import org.openl.rules.data.ITable;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.binding.wrapper.base.WrapperValidation;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

public class DataOpenFieldWrapper extends DataOpenField {
    static {
        WrapperValidation.validateWrapperClass(DataOpenFieldWrapper.class, DataOpenFieldWrapper.class.getSuperclass());
    }

    private final DataOpenField delegate;
    private final IOpenClass type;

    public DataOpenFieldWrapper(DataOpenField delegate, IOpenClass type) {
        this.delegate = delegate;
        this.type = type;
    }

    public DataOpenField getDelegate() {
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
        return delegate.get(target, env);
    }

    @Override
    public void set(Object target, Object value, IRuntimeEnv env) {
        delegate.set(target, value, env);
    }

    @Override
    public XlsNodeTypes getNodeType() {
        return delegate.getNodeType();
    }

    @Override
    public String getUri() {
        return delegate.getUri();
    }
}
