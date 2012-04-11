package org.openl.rules.table;

import org.openl.rules.lang.xls.binding.TableProperties;

public interface ITable {

    IGridTable getGridTable();

    IGridTable getGridTable(String view);

    TableProperties getProperties();

    String getType();
}
