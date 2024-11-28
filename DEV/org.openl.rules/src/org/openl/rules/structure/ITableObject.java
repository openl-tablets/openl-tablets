package org.openl.rules.structure;

import org.openl.rules.table.ILogicalTable;

public interface ITableObject {

    void addParsedTable(String name, ILogicalTable table);

    ILogicalTable getParsedTable(String name);
}
