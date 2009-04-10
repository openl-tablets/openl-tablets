package org.openl.rules.search;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IGridTable;
import org.openl.types.IOpenClass;

public class TableSearchInfo implements ITableSearchInfo {

    TableSyntaxNode tsn;
    IGridTable table;

    public TableSearchInfo(TableSyntaxNode tsn) {
        this.tsn = tsn;
        this.table = tsn.getTableBody().getGridTable();
    }

    public int numberOfColumns() {
        return table.getGridWidth();
    }

    public int numberOfRows() {
        return table.getGridHeight();
    }

    public IGridTable rowTable(int row) {
        return table.getLogicalRow(row).getGridTable();
    }

    public Object tableValue(int col, int row) {
        return table.getObjectValue(col, row);
    }

    public TableSyntaxNode getTableSyntaxNode() {
        return tsn;
    }

    public String columnDisplay(int n) {
        return null;
    }

    public String columnName(int n) {
        return null;
    }

    public IOpenClass columnType(int n) {
        return null;
    }

    public IGridTable headerDisplayTable() {
        return null;
    }

}
