package org.openl.rules.indexer;

import java.util.Vector;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;

public class TableIndexParser implements IIndexParser {

    public String getCategory() {
        return IDocumentType.WORKSHEET_TABLE.getCategory();
    }

    public String getType() {
        return "All";
    }

    public IIndexElement[] parse(IIndexElement root) {
        TableSyntaxNode tableSrc = (TableSyntaxNode) root;

        IGridTable table = tableSrc.getTable().getGridTable();

        int w = table.getLogicalWidth();
        int h = table.getLogicalHeight();

        Vector v = new Vector();

        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                if (table.getStringValue(j, i) != null) {
                    v.add(new GridCellSourceCodeModule(table, j, i
                    // , tableSrc
                            ));
                }
            }
        }

        return (GridCellSourceCodeModule[]) v.toArray(new GridCellSourceCodeModule[v.size()]);
    }

}
