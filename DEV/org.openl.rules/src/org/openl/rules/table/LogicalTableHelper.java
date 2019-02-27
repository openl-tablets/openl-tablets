package org.openl.rules.table;

/**
 * Helper class, that provides methods for creation logical tables and calculating logical columns and rows.
 *
 */
public class LogicalTableHelper {

    private LogicalTableHelper() {
    }

    /**
     * Gets the number of logical columns in the first table row.<br>
     * Each not merged cell is one logical column, several merged horizontal cells are one logical column too.
     * 
     * @param table Original source grid table.
     * @return number of logical columns in the first table row.
     */
    static int calcLogicalColumns(IGridTable table) {
        int W = table.getWidth();
        if (W == 1)
            return 1;

        int columns = 0;

        int cellWidth;
        for (int w = 0; w < W; w += cellWidth, columns++) {
            cellWidth = table.getCell(w, 0).getWidth();
        }
        return columns;
    }

    /**
     * Gets the number of logical rows in the first table column.<br>
     * Each not merged cell is one logical row, several merged vertical cells are one logical row too.
     * 
     * @param table Original source grid table.
     * @return number of logical rows in the first table column.
     */
    static int calcLogicalRows(IGridTable table) {
        int H = table.getHeight();
        if (H == 1)
            return 1;
        int rows = 0;
        int cellHeight;
        for (int h = 0; h < H; h += cellHeight, rows++) {
            cellHeight = table.getCell(0, h).getHeight();
        }
        return rows;
    }

    public static ILogicalTable logicalTable(IGridTable table,
            ILogicalTable columnOffsetsTable,
            ILogicalTable rowOffsetsTable) {
        int[] columnOffsets = null;
        if (columnOffsetsTable instanceof LogicalTable) {
            columnOffsets = ((LogicalTable) columnOffsetsTable).getColumnOffset();
        }

        int[] rowOffsets = null;
        if (rowOffsetsTable instanceof LogicalTable) {
            rowOffsets = ((LogicalTable) rowOffsetsTable).getRowOffset();
        }

        if (rowOffsets == null && columnOffsets == null)
            return LogicalTableHelper.logicalTable(table);

        return new LogicalTable(table, columnOffsets, rowOffsets);
    }

    /**
     * If there is no merged cells in the top row and left column - returns {@link SimpleLogicalTable} in other case
     * return {@link LogicalTable}
     * 
     * @param table Original source grid table.
     * @return {@link ILogicalTable} table with correctly calculated height and width.
     */
    public static ILogicalTable logicalTable(IGridTable table) {
        int width = calcLogicalColumns(table);
        int height = calcLogicalRows(table);
        if (width == table.getWidth() && height == table.getHeight()) {
            return new SimpleLogicalTable(table);
        }

        return new LogicalTable(table, width, height);
    }

    /**
     * This method will produce a logical table defined by 2 tables: leftRows and topColumns Both tables are logical
     * tables. Rows in a new table will be defined by rows in leftRows table, and columns by the columns topColumns
     * table. "Left" and "top" points to relative location of defining tables. It should be used only with "normal"
     * orientation
     *
     * @param leftRows
     * @param topColumns
     * @return
     */
    public static ILogicalTable mergeBounds(ILogicalTable leftRows, ILogicalTable topColumns) {
        IGridTable leftRowsGrid = leftRows.getSource();
        if (!leftRowsGrid.isNormalOrientation()) {
            throw new RuntimeException("Left Rows must have Normal Orientation");
        }

        IGridTable topColumnsGrid = topColumns.getSource();
        if (!topColumnsGrid.isNormalOrientation()) {
            throw new RuntimeException("Top Columns must have Normal Orientation");
        }

        IGridRegion leftRowsRegion = leftRowsGrid.getRegion();
        IGridRegion topColumnsRegion = topColumnsGrid.getRegion();

        int rLeft = leftRowsRegion.getRight() + 1;
        int cLeft = topColumnsRegion.getLeft();
        int left = cLeft;
        int startColumn = 0;
        if (cLeft < rLeft) {
            startColumn = topColumns.findColumnStart(rLeft - cLeft);
            left = rLeft;
        }

        int rTop = leftRowsRegion.getTop();
        int cTop = topColumnsRegion.getBottom() + 1;
        int top = rTop;
        int startRow = 0;
        if (rTop < cTop) {
            startRow = leftRows.findRowStart(cTop - rTop);
            top = cTop;
        }

        int right = topColumnsRegion.getRight();
        int bottom = leftRowsRegion.getBottom();

        if (right < left) {
            throw new RuntimeException("Invalid horizontal dimension");
        }

        if (bottom < top) {
            throw new RuntimeException("Invalid vertical dimension");
        }

        IGridTable gt = new GridTable(top, left, bottom, right, leftRowsGrid.getGrid());

        int nRows = leftRows.getHeight() - startRow;
        int nColumns = topColumns.getWidth() - startColumn;

        if (gt.getHeight() == nRows && gt.getWidth() == nColumns) {
            // TODO Light delegator
            return new SimpleLogicalTable(gt);
            // return new LogicalTable(gt, nColumns, nRows);
        }

        int[] rowsOffset = new int[nRows + 1];
        int[] columnsOffset = new int[nColumns + 1];
        int rOffset = 0;
        int i = 0;
        for (; i < nRows; i++) {
            rowsOffset[i] = rOffset;
            rOffset += leftRows.getRowHeight(i + startRow);
        }
        rowsOffset[i] = rOffset;

        int cOffset = 0;
        i = 0;
        for (; i < nColumns; i++) {
            columnsOffset[i] = cOffset;
            cOffset += topColumns.getColumnWidth(i + startColumn);
        }
        columnsOffset[i] = cOffset;

        return new LogicalTable(gt, columnsOffset, rowsOffset);
    }

    /**
     *
     * @return table with 1 column, if necessary transposed, caller is responsible to check that table is either 1xN or
     *         Nx1
     */
    public static ILogicalTable make1ColumnTable(ILogicalTable t) {
        if (t.getWidth() == 1) {
            return t;
        }

        if (t.getHeight() == 1) {
            return t.transpose();
        }

        // caller is responsible to check that table is either 1xN or Nx1
        return t;

    }

    public static ILogicalTable unmergeColumns(ILogicalTable table, int fromColumn, int toColumn) {

        IGridTable gt = table.getSource();

        int gridWidth = gt.getWidth();

        if (table.getWidth() == gridWidth)
            return table;

        int[] columnOffsets = getColumnOffsets(table);

        int gridFromOffset = columnOffsets[fromColumn];

        int gridToOffset = columnOffsets[toColumn];

        if (gridToOffset - gridFromOffset == toColumn - fromColumn)
            return table;

        int gridColumnsToUnmerge = gridToOffset - gridFromOffset;

        int restOfColumns = table.getWidth() - toColumn;

        int newWidth = fromColumn + gridColumnsToUnmerge + restOfColumns;

        int[] newColumnOffsets = new int[newWidth + 1];

        System.arraycopy(columnOffsets, 0, newColumnOffsets, 0, fromColumn); // copy beginning

        int offset = columnOffsets[fromColumn];

        for (int i = 0; i < gridColumnsToUnmerge; ++i, ++offset) {
            newColumnOffsets[fromColumn + i] = offset;
        }

        System
            .arraycopy(columnOffsets, toColumn, newColumnOffsets, fromColumn + gridColumnsToUnmerge, restOfColumns + 1); // copy
                                                                                                                         // the
                                                                                                                         // rest+1

        return new LogicalTable(gt, newColumnOffsets, getRowOffsets(table));
    }

    private static int[] getRowOffsets(ILogicalTable table) {
        if (table instanceof LogicalTable) {
            return ((LogicalTable) table).getRowOffset();
        }

        return calculateRowOffsets(table.getHeight(), table.getSource());
    }

    private static int[] getColumnOffsets(ILogicalTable table) {

        if (table instanceof LogicalTable) {
            return ((LogicalTable) table).getColumnOffset();
        }

        return calculateColumnOffsets(table.getWidth(), table.getSource());

    }

    static int[] calculateColumnOffsets(int width, IGridTable gt) {
        int[] columnOffset = new int[width + 1];
        int offset = 0;

        for (int i = 0, cellWidth = 0; i < width; offset += cellWidth, ++i) {
            columnOffset[i] = offset;
            cellWidth = gt.getCell(offset, 0).getWidth();
        }

        columnOffset[width] = offset; // last+1 column offset is needed to determine last column's width
        return columnOffset;
    }

    static int[] calculateRowOffsets(int height, IGridTable gt) {
        int[] rowOffset = new int[height + 1];
        int offset = 0;

        for (int i = 0, cellHeight = 0; i < height; offset += cellHeight, ++i) {
            rowOffset[i] = offset;
            cellHeight = gt.getCell(0, offset).getHeight();
        }
        rowOffset[height] = offset;
        return rowOffset;
    }
}
