package org.openl.rules.table;

/**
 * Helper class, that provides methods for creation logical tables and calculating logical columns and rows.
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
        var W = table.getWidth();
        if (W == 1) {
            return 1;
        }

        var columns = 0;

        int cellWidth;
        for (var w = 0; w < W; w += cellWidth, columns++) {
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
        var H = table.getHeight();
        if (H == 1) {
            return 1;
        }
        var rows = 0;
        int cellHeight;
        for (var h = 0; h < H; h += cellHeight, rows++) {
            cellHeight = table.getCell(0, h).getHeight();
        }
        return rows;
    }

    public static ILogicalTable logicalTable(IGridTable table,
                                             ILogicalTable columnOffsetsTable,
                                             ILogicalTable rowOffsetsTable) {
        int[] columnOffsets = null;
        if (columnOffsetsTable instanceof LogicalTable logicalTable) {
            columnOffsets = logicalTable.getColumnOffset();
        }

        int[] rowOffsets = null;
        if (rowOffsetsTable instanceof LogicalTable logicalTable) {
            rowOffsets = logicalTable.getRowOffset();
        }

        if (rowOffsets == null && columnOffsets == null) {
            return LogicalTableHelper.logicalTable(table);
        }

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
        var width = calcLogicalColumns(table);
        var height = calcLogicalRows(table);
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
        var leftRowsGrid = leftRows.getSource();
        if (!leftRowsGrid.isNormalOrientation()) {
            throw new RuntimeException("Left Rows must have Normal Orientation");
        }

        var topColumnsGrid = topColumns.getSource();
        if (!topColumnsGrid.isNormalOrientation()) {
            throw new RuntimeException("Top Columns must have Normal Orientation");
        }

        var leftRowsRegion = leftRowsGrid.getRegion();
        var topColumnsRegion = topColumnsGrid.getRegion();

        var rLeft = leftRowsRegion.getRight() + 1;
        var cLeft = topColumnsRegion.getLeft();
        var left = cLeft;
        var startColumn = 0;
        if (cLeft < rLeft) {
            startColumn = topColumns.findColumnStart(rLeft - cLeft);
            left = rLeft;
        }

        var rTop = leftRowsRegion.getTop();
        var cTop = topColumnsRegion.getBottom() + 1;
        var top = rTop;
        var startRow = 0;
        if (rTop < cTop) {
            startRow = leftRows.findRowStart(cTop - rTop);
            top = cTop;
        }

        var right = topColumnsRegion.getRight();
        var bottom = leftRowsRegion.getBottom();

        if (right < left) {
            throw new RuntimeException("Invalid horizontal dimension");
        }

        if (bottom < top) {
            throw new RuntimeException("Invalid vertical dimension");
        }

        var gt = new GridTable(top, left, bottom, right, leftRowsGrid.getGrid());

        var nRows = leftRows.getHeight() - startRow;
        var nColumns = topColumns.getWidth() - startColumn;

        if (gt.getHeight() == nRows && gt.getWidth() == nColumns) {
            // TODO Light delegator
            return new SimpleLogicalTable(gt);
            // return new LogicalTable(gt, nColumns, nRows);
        }

        int[] rowsOffset = new int[nRows + 1];
        int[] columnsOffset = new int[nColumns + 1];
        var rOffset = 0;
        var i = 0;
        for (; i < nRows; i++) {
            rowsOffset[i] = rOffset;
            rOffset += leftRows.getRowHeight(i + startRow);
        }
        rowsOffset[i] = rOffset;

        var cOffset = 0;
        i = 0;
        for (; i < nColumns; i++) {
            columnsOffset[i] = cOffset;
            cOffset += topColumns.getColumnWidth(i + startColumn);
        }
        columnsOffset[i] = cOffset;

        return new LogicalTable(gt, columnsOffset, rowsOffset);
    }

    /**
     * @return table with 1 column, if necessary transposed, caller is responsible to check that table is either 1xN or
     * Nx1
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

        var gt = table.getSource();

        var gridWidth = gt.getWidth();

        if (table.getWidth() == gridWidth) {
            return table;
        }

        int[] columnOffsets = getColumnOffsets(table);

        var gridFromOffset = columnOffsets[fromColumn];

        var gridToOffset = columnOffsets[toColumn];

        if (gridToOffset - gridFromOffset == toColumn - fromColumn) {
            return table;
        }

        var gridColumnsToUnmerge = gridToOffset - gridFromOffset;

        var restOfColumns = table.getWidth() - toColumn;

        var newWidth = fromColumn + gridColumnsToUnmerge + restOfColumns;

        int[] newColumnOffsets = new int[newWidth + 1];

        System.arraycopy(columnOffsets, 0, newColumnOffsets, 0, fromColumn); // copy beginning

        var offset = columnOffsets[fromColumn];

        for (var i = 0; i < gridColumnsToUnmerge; ++i, ++offset) {
            newColumnOffsets[fromColumn + i] = offset;
        }

        System
                .arraycopy(columnOffsets, toColumn, newColumnOffsets, fromColumn + gridColumnsToUnmerge, restOfColumns + 1); // copy
        // the
        // rest+1

        return new LogicalTable(gt, newColumnOffsets, getRowOffsets(table));
    }

    private static int[] getRowOffsets(ILogicalTable table) {
        if (table instanceof LogicalTable logicalTable) {
            return logicalTable.getRowOffset();
        }

        return calculateRowOffsets(table.getHeight(), table.getSource());
    }

    private static int[] getColumnOffsets(ILogicalTable table) {

        if (table instanceof LogicalTable logicalTable) {
            return logicalTable.getColumnOffset();
        }

        return calculateColumnOffsets(table.getWidth(), table.getSource());

    }

    static int[] calculateColumnOffsets(int width, IGridTable gt) {
        int[] columnOffset = new int[width + 1];
        var offset = 0;

        for (int i = 0, cellWidth; i < width; offset += cellWidth, ++i) {
            columnOffset[i] = offset;
            cellWidth = gt.getCell(offset, 0).getWidth();
        }

        columnOffset[width] = offset; // last+1 column offset is needed to determine last column's width
        return columnOffset;
    }

    static int[] calculateRowOffsets(int height, IGridTable gt) {
        int[] rowOffset = new int[height + 1];
        var offset = 0;

        for (int i = 0, cellHeight; i < height; offset += cellHeight, ++i) {
            rowOffset[i] = offset;
            cellHeight = gt.getCell(0, offset).getHeight();
        }
        rowOffset[height] = offset;
        return rowOffset;
    }
}
