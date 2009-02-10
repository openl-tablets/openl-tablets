package org.openl.rules.cmatch;

import java.util.List;

public class ColumnMatchAlgorithm {
    public void compile(ColumnMatch columnMatch) {
        List<TableRow> rows = columnMatch.getRows();
        List<TableColumn> columns = columnMatch.getColumns();

        for (int r = 0; r < rows.size(); r++) {
            System.out.println("row #" + r);
            TableRow row = rows.get(r);
            
            for (TableColumn c : columns) {
                System.out.println("  column " + c.getId());
                System.out.print("   ");
                SubValue[] values = row.get(c.getId());
                
                for (SubValue sv : values) {
                    System.out.print(" " + sv.getIndent() + ":" + sv.getString());
                }
                System.out.println();
            }
        }
    }
}
