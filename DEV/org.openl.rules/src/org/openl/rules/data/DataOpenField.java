package org.openl.rules.data;

import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.types.IUriMember;
import org.openl.types.IDynamicObject;
import org.openl.types.IOpenClass;
import org.openl.types.impl.AOpenField;
import org.openl.vm.IRuntimeEnv;

public class DataOpenField extends AOpenField implements IUriMember {

    private ITable table;
    private Object data;
    private ModuleOpenClass declaringClass;
    private String uri;
    private XlsNodeTypes nodeType;

    public DataOpenField() {
        super(null, null);
    }

    public DataOpenField(ITable table, ModuleOpenClass declaringClass) {

        super(table.getDataModel().getName(),
            table.getDataModel().getType().getAggregateInfo().getIndexedAggregateType(table.getDataModel().getType()));

        this.table = table;
        this.uri = table.getTableSyntaxNode().getTable().getSource().getUri();
        data = table.getDataArray();
        this.nodeType = table.getTableSyntaxNode().getNodeType();
        this.declaringClass = declaringClass;
    }

    @Override
    public String getUri() {
        return uri;
    }

    @Override
    public IOpenClass getDeclaringClass() {
        return declaringClass;
    }

    public ITable getTable() {
        return table;
    }

    public Object getData() {
        return data;
    }

    public void setTable(ITable table) {
        this.table = table;
    }

    @Override
    public boolean isWritable() {
        return true;
    }

    @Override
    public Object get(Object target, IRuntimeEnv env) {
        if (target == null) {
            return getType().nullObject();
        }
        Object data = ((IDynamicObject) target).getFieldValue(getName());

        if (data == null) {
            data = this.data;
            ((IDynamicObject) target).setFieldValue(getName(), data);
        }

        return data;
    }

    @Override
    public void set(Object target, Object value, IRuntimeEnv env) {
        if (target != null) {
            ((IDynamicObject) target).setFieldValue(getName(), value);
        }
    }

    public XlsNodeTypes getNodeType() {
        return nodeType;
    }
}
