package org.openl.rules.table;

import lombok.Getter;

/**
 * Fully implementation for {@link ILogicalTable} interface.<br>
 * Logical Table consists of logical columns and rows (created as a result of merged cells). Each merged region is taken
 * as one cell.<br>
 * Use {@link LogicalTableHelper#logicalTable(IGridTable)}
 * {@link LogicalTableHelper#logicalTable(IGridTable, ILogicalTable, ILogicalTable)} to correctly construct this object.
 *
 * @author snshor
 */
public class LogicalTable extends ALogicalTable {

    @Getter
    private final int[] rowOffset;

    @Getter
    private final int[] columnOffset;

    public LogicalTable(IGridTable table, int width, int height) {
        super(table);
        this.rowOffset = LogicalTableHelper.calculateRowOffsets(height, table);
        this.columnOffset = LogicalTableHelper.calculateColumnOffsets(width, table);
    }

    public LogicalTable(IGridTable table, int[] columnOffset, int[] rowOffset) {
        super(table);

        if (columnOffset == null) {
            var width = LogicalTableHelper.calcLogicalColumns(table);
            this.columnOffset = LogicalTableHelper.calculateColumnOffsets(width, table);
        } else {
            this.columnOffset = columnOffset;
        }

        if (rowOffset == null) {
            var height = LogicalTableHelper.calcLogicalRows(table);
            this.rowOffset = LogicalTableHelper.calculateRowOffsets(height, table);
        } else {
            this.rowOffset = rowOffset;
        }
    }

    @Override
    public int getWidth() {
        return columnOffset.length - 1;
    }

    @Override
    public int getHeight() {
        return rowOffset.length - 1;
    }

    @Override
    public int findColumnStart(int gridOffset) {
        for (var i = 0; i < columnOffset.length - 1; i++) {
            if (columnOffset[i] == gridOffset) {
                return i;
            }
            if (columnOffset[i] > gridOffset) {
                throw new TableException("gridOffset does not match column start");
            }
        }
        throw new TableException("gridOffset is higher than table's width");
    }

    @Override
    public int findRowStart(int gridOffset) {
        for (var i = 0; i < rowOffset.length - 1; i++) {
            if (rowOffset[i] == gridOffset) {
                return i;
            }
            if (rowOffset[i] > gridOffset) {
                throw new TableException("gridOffset does not match row start");
            }
        }
        throw new TableException("gridOffset is higher than table's height");
    }

    @Override
    public int getColumnWidth(int column) {
        return columnOffset[column + 1] - columnOffset[column];
    }

    @Override
    public int getRowHeight(int row) {
        return rowOffset[row + 1] - rowOffset[row];
    }

    @Override
    public ILogicalTable getSubtable(int column, int row, int width, int height) {
        if (width == 0 || height == 0) {
            return null;
        }
        var startRow = rowOffset[row];
        var endRow = rowOffset[row + height];
        var startColumn = columnOffset[column];
        var endColumn = columnOffset[column + width];

        return LogicalTableHelper
                .logicalTable(getSource().getSubtable(startColumn, startRow, endColumn - startColumn, endRow - startRow));
    }

    @Override
    public ICell getCell(int column, int row) {
        var r = rowOffset[row];
        var c = columnOffset[column];
        return getSource().getCell(c, r);
    }
}
