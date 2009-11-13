package org.openl.rules.table;

import org.openl.rules.table.properties.ITableProperties;

public interface ITable {

    IGridTable getGridTable();

    IGridTable getGridTable(String view);

    ITableProperties getProperties();

    String getType();
}
