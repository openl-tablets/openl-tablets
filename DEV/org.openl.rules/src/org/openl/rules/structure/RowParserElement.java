package org.openl.rules.structure;

import org.openl.rules.table.ILogicalTable;

public class RowParserElement extends ATableParserElement {

    @Override
    protected ILogicalTable parseInternal(ILogicalTable unparsedTable, ITableObject tobj) {
        ILogicalTable row = unparsedTable.getRow(0);

        tobj.addParsedTable(name, row);
        return unparsedTable.getRows(1);
    }

}
