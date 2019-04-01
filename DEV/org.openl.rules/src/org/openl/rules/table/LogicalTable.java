package org.openl.rules.table;

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

    private int[] rowOffset;

    private int[] columnOffset;

    public LogicalTable(IGridTable table, int width, int height) {
        super(table);
        this.rowOffset = LogicalTableHelper.calculateRowOffsets(height, table);
        this.columnOffset = LogicalTableHelper.calculateColumnOffsets(width, table);
    }

    public LogicalTable(IGridTable table, int[] columnOffset, int[] rowOffset) {
        super(table);

        if (columnOffset == null) {
            int width = LogicalTableHelper.calcLogicalColumns(table);
            this.columnOffset = LogicalTableHelper.calculateColumnOffsets(width, table);
        } else {
            this.columnOffset = columnOffset;
        }

        if (rowOffset == null) {
            int height = LogicalTableHelper.calcLogicalRows(table);
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
        for (int i = 0; i < columnOffset.length - 1; i++) {
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
        for (int i = 0; i < rowOffset.length - 1; i++) {
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
        int startRow = rowOffset[row];
        int endRow = rowOffset[row + height];
        int startColumn = columnOffset[column];
        int endColumn = columnOffset[column + width];

        return LogicalTableHelper
            .logicalTable(getSource().getSubtable(startColumn, startRow, endColumn - startColumn, endRow - startRow));
    }

    public int[] getRowOffset() {
        return rowOffset;
    }

    public int[] getColumnOffset() {
        return columnOffset;
    }

    @Override
    public ICell getCell(int column, int row) {
        int r = rowOffset[row];
        int c = columnOffset[column];
        return getSource().getCell(c, r);
    }
}
