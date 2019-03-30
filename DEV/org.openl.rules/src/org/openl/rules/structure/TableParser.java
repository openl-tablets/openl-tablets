package org.openl.rules.structure;

import org.openl.rules.table.ILogicalTable;

public final class TableParser {

    private TableParser() {
    }

    public static void parseTable(ILogicalTable table, ITableParserElement[] pElements, ITableObject tobj) {
        ILogicalTable unparsedTable = table;

        for (int i = 0; i < pElements.length; i++) {
            unparsedTable = pElements[i].parse(unparsedTable, tobj);
        }

    }

}
