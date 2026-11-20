package org.openl.rules.data;

import lombok.Getter;
import lombok.Setter;

import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.types.IDynamicObject;
import org.openl.types.IOpenClass;
import org.openl.types.impl.AOpenField;
import org.openl.vm.IRuntimeEnv;

public class DataOpenField extends AOpenField {

    @Getter
    @Setter
    private ITable table;
    @Getter
    private Object data;
    private ModuleOpenClass declaringClass;
    @Getter
    private XlsNodeTypes nodeType;
    @Getter
    private String uri;

    public DataOpenField() {
        super(null, null);
    }

    public DataOpenField(ITable table, ModuleOpenClass declaringClass) {
        this(table.getDataModel().getName(), table.getDataModel().getType(), table, declaringClass);
        this.data = table.getDataArray();
    }

    DataOpenField(String name, IOpenClass tableType, ITable table, ModuleOpenClass declaringClass) {
        super(name, tableType.getAggregateInfo().getIndexedAggregateType(tableType));
        this.table = table;
        this.nodeType = table.getTableSyntaxNode().getNodeType();
        this.declaringClass = declaringClass;
        this.uri = table.getTableSyntaxNode().getUri();
    }

    boolean fill(ITable boundTable) {
        if (table != boundTable) {
            return false;
        }
        data = boundTable.getDataArray();
        return true;
    }

    @Override
    public IOpenClass getDeclaringClass() {
        return declaringClass;
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
        var dynamicObject = ((IDynamicObject) target).getFieldValue(getName());

        if (dynamicObject == null) {
            dynamicObject = this.data;
            ((IDynamicObject) target).setFieldValue(getName(), dynamicObject);
        }

        return dynamicObject;
    }

    @Override
    public void set(Object target, Object value, IRuntimeEnv env) {
        if (target != null) {
            ((IDynamicObject) target).setFieldValue(getName(), value);
        }
    }
}
