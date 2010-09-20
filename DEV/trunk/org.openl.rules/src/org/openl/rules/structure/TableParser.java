package org.openl.rules.structure;

import org.openl.rules.table.IGridTable;

public class TableParser {

    public static void parseTable(IGridTable table, ITableParserElement[] pElements, ITableObject tobj) {
        IGridTable unparsedTable = table;

        for (int i = 0; i < pElements.length; i++) {
            unparsedTable = pElements[i].parse(unparsedTable, tobj);
        }

    }

}
