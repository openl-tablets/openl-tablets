package org.openl.rules.table;

/**
 * Fully implementation for {@link ILogicalTable} interface.<br>
 * Logical Table consists of logical columns and rows (created as a result of
 * merged cells). Each merged region is taken as one cell.<br> 
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
        calculateRowOffsets(height);
        calculateColumnOffsets(width);
    }

    public LogicalTable(IGridTable table, int[] columnOffset, int[] rowOffset) {
        super(table);

        if (columnOffset == null) {
            int width = LogicalTableHelper.calcLogicalColumns(table);
            calculateColumnOffsets(width);
        }
        else
            this.columnOffset = columnOffset;

        if (rowOffset == null) {
            int height = LogicalTableHelper.calcLogicalRows(table);
            calculateRowOffsets(height);
        }
        else
            this.rowOffset = rowOffset;
    }

    private void calculateRowOffsets(int height) {
        rowOffset = new int[height + 1];
        int cellHeight = 0, offset = 0;
        int i = 0;
        for (; i < rowOffset.length - 1; offset += cellHeight, ++i) {
            rowOffset[i] = offset;
            cellHeight = table.getCell(0, offset).getHeight();
        }
        rowOffset[i] = offset;
    }

    private void calculateColumnOffsets(int width) {
        columnOffset = new int[width+1];
        int cellWidth = 0;
        int offset = 0;
        int i = 0;
        for (; i < columnOffset.length - 1; offset += cellWidth, ++i) {
            columnOffset[i] = offset;
            cellWidth = table.getCell(offset, 0).getWidth();
        }
        columnOffset[i] = offset;
    }

    public int getWidth() {
        return columnOffset.length - 1;
    }

    public int getHeight() {
        return rowOffset.length - 1;
    }

    public int findColumnStart(int gridOffset) throws TableException {
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

    public int findRowStart(int gridOffset) throws TableException {
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

    public int getColumnWidth(int column) {
        return columnOffset[column + 1] - columnOffset[column];
    }

    public int getRowHeight(int row) {
        return rowOffset[row + 1] - rowOffset[row];
    }

    public ILogicalTable getSubtable(int column, int row, int width, int height) {
        int startRow = rowOffset[row];
        int endRow = rowOffset[row + height];
        int startColumn = columnOffset[column];
        int endColumn = columnOffset[column + width];

        return LogicalTableHelper.logicalTable(
                table.getSubtable(startColumn, startRow, endColumn - startColumn, endRow - startRow));
    }

    public int[] getRowOffset() {
        return rowOffset;
    }

    public int[] getColumnOffset() {
        return columnOffset;
    }

}
