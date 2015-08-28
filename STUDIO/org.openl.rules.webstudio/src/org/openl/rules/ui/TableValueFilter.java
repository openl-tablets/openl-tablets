package org.openl.rules.ui;

import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.table.FormattedCell;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.ui.filters.AGridFilter;

class TableValueFilter extends AGridFilter {

    private SpreadsheetResult res;

    private int startX, startY;

    public TableValueFilter(final SpreadsheetResult res) {
        this.res = res;
        ILogicalTable table = res.getLogicalTable();

        IGridTable t = table.getSource();

        this.startX = t.getGridColumn(0, 0) + table.getColumn(0).getSource().getWidth();
        this.startY = t.getGridRow(0, 0) + table.getRow(0).getSource().getHeight();
    }

    public FormattedCell filterFormat(FormattedCell cell) {
        ILogicalTable table = res.getLogicalTable();

        int columnOffset = 0;
        int rowOffset = 0;

        for (int i = 1; i < cell.getColumn() - startX; i++) {
            columnOffset += table.getColumn(i).getSource().getWidth() - 1;
        }

        for (int i = 1; i < cell.getRow() - startY; i++) {
            rowOffset += table.getRow(i).getSource().getHeight() - 1;
        }

        int col = cell.getColumn() - startX - columnOffset;
        int row = cell.getRow() - startY - rowOffset;
        if (row >= 0 && col >= 0 && res.getWidth() > col && res.getHeight() > row) {
            Object v = res.getValue(row, col);
            if (v != null) {
                cell.setObjectValue(v);
                cell.setFormattedValue(String.valueOf(v));
            }
        }

        return cell;
    }

}
