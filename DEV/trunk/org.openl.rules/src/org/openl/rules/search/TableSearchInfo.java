package org.openl.rules.search;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IGridTable;
import org.openl.types.IOpenClass;

public class TableSearchInfo implements ITableSearchInfo {

    private TableSyntaxNode tsn;
    private IGridTable table;

    public TableSearchInfo(TableSyntaxNode tsn) {
        this.tsn = tsn;
        table = tsn.getTableBody().getSource();
    }

    public String getColumnDisplay(int col) {
        return null;
    }

    public String getColumnName(int col) {
        return null;
    }

    public IOpenClass getColumnType(int col) {
        return null;
    }

    public TableSyntaxNode getTableSyntaxNode() {
        return tsn;
    }

    public IGridTable getHeaderDisplayTable() {
        return null;
    }

    public int getNumberOfColumns() {
        return table.getWidth();
    }

    public int getNumberOfRows() {
        return table.getHeight();
    }

    public IGridTable getRowTable(int row) {
        return table.getRow(row);
    }

    public Object getTableValue(int col, int row) {
        return table.getCell(col, row).getObjectValue();
    }

}
