package org.openl.rules.structure;

import org.openl.rules.table.IGridTable;

public class RowParserElement extends ATableParserElement {

    @Override
    protected IGridTable parseInternal(IGridTable unparsedTable, ITableObject tobj) {
        IGridTable row = unparsedTable.getRow(0);

        tobj.addParsedTable(name, row);
        return unparsedTable.rows(1);
    }

}
