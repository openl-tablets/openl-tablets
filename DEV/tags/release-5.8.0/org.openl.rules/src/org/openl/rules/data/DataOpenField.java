package org.openl.rules.data;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.types.IDynamicObject;
import org.openl.types.impl.AOpenField;
import org.openl.vm.IRuntimeEnv;

public class DataOpenField extends AOpenField {

    private ITable table;
    private Object data;

    public DataOpenField(ITable table, TableSyntaxNode tableSyntaxNode) {

        super(table.getDataModel().getName(), table.getDataModel()
            .getType()
            .getAggregateInfo()
            .getIndexedAggregateType(table.getDataModel().getType(), 1));
        
        this.table = table;
        data = table.getDataArray();
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

    public Object get(Object target, IRuntimeEnv env) {

        Object data = ((IDynamicObject) target).getFieldValue(getName());

        if (data == null) {
            data = this.data;
            ((IDynamicObject) target).setFieldValue(getName(), data);
        }

        return data;
    }

    public void set(Object target, Object value, IRuntimeEnv env) {
        ((IDynamicObject) target).setFieldValue(getName(), value);
    }

}
