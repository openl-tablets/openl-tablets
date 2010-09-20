package org.openl.rules.structure;

import org.openl.rules.table.IGridTable;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;

public class TableObjectDelegator implements ITableObject {

    Object target;
    IOpenClass openClass;

    public void addParsedTable(String name, IGridTable table) {
        IOpenField field = openClass.getField(name, true);
        field.set(target, table, null);
    }

    public IGridTable getParsedTable(String name) {
        IOpenField field = openClass.getField(name, true);
        return (IGridTable) field.get(target, null);
    }

}
