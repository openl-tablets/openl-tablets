package org.openl.rules.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.helpers.NumberUtils;
import org.openl.rules.table.FormattedCell;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.formatters.FormattersManager;
import org.openl.rules.table.ui.filters.AGridFilter;
import org.openl.util.formatters.IFormatter;

class TableValueFilter extends AGridFilter {

    private final SpreadsheetResult res;

    private final int startX;
    private final int startY;
    private final boolean smartNumbers;

    public TableValueFilter(final SpreadsheetResult res, boolean smartNumbers) {
        this.res = res;
        ILogicalTable table = res.getLogicalTable();

        IGridTable t = table.getSource();

        this.startX = t.getGridColumn(0, 0) + table.getColumn(0).getSource().getWidth();
        this.startY = t.getGridRow(0, 0) + table.getRow(0).getSource().getHeight();
        this.smartNumbers = smartNumbers;
    }

    @Override
    public FormattedCell filterFormat(FormattedCell cell) {
        ILogicalTable table = res.getLogicalTable();

        int relativeColumn = cell.getColumn() - startX;
        int relativeRow = cell.getRow() - startY;
        if (relativeColumn < 0 || relativeColumn >= table.getWidth() || relativeRow < 0 || relativeRow >= table
                .getHeight()) {
            // Sometimes the style of a cell outside of a table is retrieved to draw borders of a table, for such cells
            // value is empty - don't modify the cell, keep empty.
            return cell;
        }

        int columnOffset = 0;
        int rowOffset = 0;

        for (int i = 1; i < relativeColumn; i++) {
            columnOffset += table.getColumn(i).getSource().getWidth() - 1;
        }

        for (int i = 1; i < relativeRow; i++) {
            rowOffset += table.getRow(i).getSource().getHeight() - 1;
        }

        int col = cell.getColumn() - startX - columnOffset;
        int row = cell.getRow() - startY - rowOffset;
        if (row >= 0 && col >= 0 && res.getWidth() > col && res.getHeight() > row) {
            Object v = res.getValue(row, col);
            cell.setObjectValue(v);
            if (v != null) {
                try {
                    String format = null;
                    if (NumberUtils.isNumberType(v.getClass()) && !smartNumbers) {
                        format = FormattersManager.DEFAULT_NUMBER_FORMAT;
                    }
                    IFormatter formatter = FormattersManager.getFormatter(v.getClass(), format);
                    cell.setFormattedValue(formatter.format(v));
                } catch (Exception | LinkageError | StackOverflowError e) {
                    Logger log = LoggerFactory.getLogger(getClass());
                    log.debug(e.getMessage(), e);
                    cell.setFormattedValue(String.format(
                            "<span style=\"color: red;\">'%s' exception has been thrown. Failed to format '%s'.</span>",
                            e.getClass().getName(),
                            v.getClass().getName()));
                }
            } else {
                cell.setFormattedValue("null");
            }
        }

        return cell;
    }

}
