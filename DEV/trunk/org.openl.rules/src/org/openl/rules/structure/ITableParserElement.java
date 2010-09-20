package org.openl.rules.structure;

import org.openl.rules.table.IGridTable;

public interface ITableParserElement {

    String getName();

    IGridTable parse(IGridTable unparsedTable, ITableObject tobj);

}
