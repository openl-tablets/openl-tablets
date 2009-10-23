package org.openl.rules.lang.xls.syntax;

import org.openl.rules.lang.xls.binding.TableProperties;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.ITable;

public class TableSyntaxNodeAdapter implements ITable {

    private TableSyntaxNode tsn;

    public TableSyntaxNodeAdapter(TableSyntaxNode tsn) {
        if (tsn == null) {
            throw new IllegalArgumentException("TableSyntaxNode is null");
        }
        this.tsn = tsn;
    }

    public IGridTable getGridTable() {
        return tsn.getTable().getGridTable();
    }

    public IGridTable getGridTable(String view) {
        if (view != null) {
            ILogicalTable gtx = tsn.getSubTables().get(view);
            if (gtx != null) {
                return gtx.getGridTable();
            }
        }
        return getGridTable();
    }

    public TableProperties getProperties() {
        return tsn.getTableProperties();
    }

    public String getType() {
        return tsn.getType();
    }
}
