package org.openl.rules.data;

import org.openl.types.impl.AOpenField;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

public class PrimaryKeyField extends AOpenField {

    private ITable table;

    public PrimaryKeyField(String name, ITable table) {
        super(name, JavaOpenClass.STRING);
        this.table = table;
    }

    @Override
    public Object get(Object target, IRuntimeEnv env) {

        Integer row = table.getRowIndex(target);
        if (row == null) {
            return null;
        }

        return table.getPrimaryIndexKey(row);
    }

    @Override
    public boolean isWritable() {
        return true;
    }

    @Override
    public void set(Object target, Object value, IRuntimeEnv env) {

        int row = table.getRowIndex(target);
        table.setPrimaryIndexKey(row, (String) value);
    }

}
