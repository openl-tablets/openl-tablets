package org.openl.rules.structure;

import org.openl.rules.table.ILogicalTable;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;

public class TableObjectDelegator implements ITableObject {

    Object target;
    IOpenClass openClass;

    public void addParsedTable(String name, ILogicalTable table) {
        IOpenField field = openClass.getField(name, true);
        field.set(target, table, null);
    }

    public ILogicalTable getParsedTable(String name) {
        IOpenField field = openClass.getField(name, true);
        return (ILogicalTable) field.get(target, null);
    }

}
