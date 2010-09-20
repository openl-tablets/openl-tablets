package org.openl.rules.structure;

import org.openl.rules.table.IGridTable;

public interface ITableObject {

    void addParsedTable(String name, IGridTable table);

    IGridTable getParsedTable(String name);
}
