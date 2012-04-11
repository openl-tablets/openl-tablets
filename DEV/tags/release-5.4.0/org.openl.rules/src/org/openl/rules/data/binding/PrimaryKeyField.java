package org.openl.rules.data.binding;

import org.openl.rules.data.ITable;
import org.openl.types.IOpenField;
import org.openl.types.impl.AOpenField;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

public class PrimaryKeyField extends AOpenField implements IOpenField {

    String name;
    ITable table;

    public PrimaryKeyField(String name, ITable table) {
        super(name, JavaOpenClass.STRING);
        this.table = table;
    }

    public Object get(Object target, IRuntimeEnv env) {
        int row = table.getRowIndex(target);
        return table.getPrimaryIndexKey(row);
        // throw new UnsupportedOperationException();
    }

    @Override
    public boolean isWritable() {
        return true;
    }

    public void set(Object target, Object value, IRuntimeEnv env) {
        int row = table.getRowIndex(target);
        table.setPrimaryIndexKey(row, (String) value);
    }

}
