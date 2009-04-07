/*
 * Created on Oct 28, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.table;

/**
 * @author snshor
 *
 */
public class LogicalTable extends ALogicalTable implements ILogicalTable {
    IGridTable gridTable;

    // ILogicalTable[] rows;
    //
    // ILogicalTable[] cols;

    int[] rowOffset;

    int[] columnOffset;

    public static int calcLogicalColumns(IGridTable gridTable) {

        int columns = 0;
        int cellWidth;
        for (int w = 0; w < gridTable.getGridWidth(); w += cellWidth, columns++) {
            cellWidth = gridTable.getCellWidth(w, 0);
        }
        return columns;
    }

    public static int calcLogicalRows(IGridTable gridTable) {

        int rows = 0;
        int cellHeight;
        for (int h = 0; h < gridTable.getGridHeight(); h += cellHeight, rows++) {
            cellHeight = gridTable.getCellHeight(0, h);
        }
        return rows;
    }

    static public ILogicalTable logicalTable(ILogicalTable table) {
        IGridTable gridTable = table.getGridTable();
        int width = calcLogicalColumns(gridTable);
        int height = calcLogicalRows(gridTable);
        if (width == gridTable.getLogicalWidth() && height == gridTable.getLogicalHeight()) {
            return gridTable;
        }

        return new LogicalTable(gridTable, width, height);
    }

    // synchronized ILogicalTable[] getRows()
    // {
    // if (rows == null)
    // {
    // Vector v = new Vector();
    // int cellHeight;
    // for (int i = 0; i < gridTable.getGridHeight(); i += cellHeight)
    // {
    // cellHeight = gridTable.getCellHeight(0, i);
    // v.add(new LogicalTable(new GridTableRows(getGridTable(), i, i +
    // cellHeight
    // - 1)));
    // }
    //
    // rows = (ILogicalTable[])v.toArray(new ILogicalTable[v.size()]);
    // }
    //
    // return rows;
    // }

    // synchronized ILogicalTable[] getColumns()
    // {
    // if (cols == null)
    // {
    // Vector v = new Vector();
    // int cellWidth;
    // for (int i = 0; i < gridTable.getGridWidth(); i += cellWidth)
    // {
    // cellWidth = gridTable.getCellWidth(i, 0);
    // v.add(new LogicalTable(new GridTableColumns(getGridTable(), i, i +
    // cellWidth - 1)));
    // }
    //
    // cols = (ILogicalTable[])v.toArray(new ILogicalTable[v.size()]);
    // }
    //
    // return cols;
    // }

    /**
     * This method will produce a logical table defined by 2 tables: leftRows
     * and topColumns Both tables are logical tables. Rows in a new table will
     * be defined by rows in leftRows table, and columns by the columns
     * topColumns table. "Left" and "top" points to relative location of
     * defining tables. It should be used only with "normal" orientation
     *
     * @param leftRows
     * @param topColumns
     * @return
     */
    public static ILogicalTable mergeBounds(ILogicalTable leftRows, ILogicalTable topColumns) {
        IGridTable leftRowsGrid = leftRows.getGridTable();
        if (!leftRowsGrid.isNormalOrientation()) {
            throw new RuntimeException("Left Rows must have Normal Orientation");
        }

        IGridTable topColumnsGrid = topColumns.getGridTable();
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

        int nRows = leftRows.getLogicalHeight() - startRow;
        int nColumns = topColumns.getLogicalWidth() - startColumn;

        if (gt.getLogicalHeight() == nRows && gt.getLogicalWidth() == nColumns) {
            return gt;
        }

        int[] rowsOffset = new int[nRows + 1];
        int[] columnsOffset = new int[nColumns + 1];
        int rOffset = 0;
        int i = 0;
        for (; i < nRows; i++) {
            rowsOffset[i] = rOffset;
            rOffset += leftRows.getLogicalRowGridHeight(i + startRow);
        }
        rowsOffset[i] = rOffset;

        int cOffset = 0;
        i = 0;
        for (; i < nColumns; i++) {
            columnsOffset[i] = cOffset;
            cOffset += topColumns.getLogicalColumnGridWidth(i + startColumn);
        }
        columnsOffset[i] = cOffset;

        return new LogicalTable(gt, columnsOffset, rowsOffset);
    }

    public LogicalTable(IGridTable gridTable, int width, int height) {
        this.gridTable = gridTable;
        rowOffset = new int[height + 1];
        columnOffset = new int[width + 1];
        calculateOffsets();
    }

    public LogicalTable(IGridTable gridTable, int[] columnOffset, int[] rowOffset) {
        this.gridTable = gridTable;
        this.rowOffset = rowOffset;
        this.columnOffset = columnOffset;
    }

    private void calculateOffsets() {
        int cellHeight = 0, offset = 0;
        int i = 0;
        for (; i < rowOffset.length - 1; offset += cellHeight, ++i) {
            rowOffset[i] = offset;
            cellHeight = gridTable.getCellHeight(0, offset);
        }
        rowOffset[i] = offset;

        int cellWidth = 0;
        offset = 0;
        i = 0;
        for (; i < columnOffset.length - 1; offset += cellWidth, ++i) {
            columnOffset[i] = offset;
            cellWidth = gridTable.getCellWidth(offset, 0);
        }
        columnOffset[i] = offset;
    }

    @Override
    protected ILogicalTable columnsInternal(int from, int to) {
        // ILogicalTable[] cols = getColumns();
        //
        // int startWidth = 0;
        // int endWidth = 0;
        // for (int i = 0; i <= to; i++)
        // {
        // int width = cols[i].getGridTable().getGridWidth();
        // if (i < from)
        // startWidth += width;
        // endWidth += width;
        // }

        return LogicalTable.logicalTable(gridTable.columns(columnOffset[from], columnOffset[to + 1] - 1));
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

    /**
     * @return
     */
    public IGridTable getGridTable() {
        return gridTable;
    }

    /**
     *
     */

    public ILogicalTable getLogicalColumn(int column) {
        return getLogicalRegion(column, 0, 1, getLogicalHeight());
    }

    public int getLogicalColumnGridWidth(int column) {

        return columnOffset[column + 1] - columnOffset[column];
    }

    /**
     *
     */

    public int getLogicalHeight() {
        return rowOffset.length - 1;
    }

    /**
     *
     */

    @Override
    public ILogicalTable getLogicalRegionInternal(int column, int row, int width, int height) {
        // int gridColumn = 0, gridRow = 0;
        // int gridHeight = 0, gridWidth = 0;
        //
        // for (int i = 0; i < row; i++)
        // {
        // gridRow += getRows()[i].getGridTable().getGridHeight();
        // }
        //
        // for (int i = 0; i < column; i++)
        // {
        // gridColumn += getColumns()[i].getGridTable().getGridWidth();
        // }
        //
        // for (int i = 0; i < height; i++)
        // {
        // gridHeight += getRows()[i + row].getGridTable().getGridHeight();
        // }
        //
        // for (int i = 0; i < width; i++)
        // {
        // gridWidth += getColumns()[i + column].getGridTable().getGridWidth();
        // }
        //
        //
        //
        // return new
        // LogicalTable((IGridTable)gridTable.getLogicalRegion(gridColumn,
        // gridRow,
        // gridWidth, gridHeight));

        int startRow = rowOffset[row];
        int endRow = rowOffset[row + height];
        int startColumn = columnOffset[column];
        int endColumn = columnOffset[column + width];

        return LogicalTable.logicalTable(gridTable.getLogicalRegion(startColumn, startRow, endColumn - startColumn,
                endRow - startRow));
    }

    /**
     *
     */

    public ILogicalTable getLogicalRow(int row) {
        return getLogicalRegion(0, row, getLogicalWidth(), 1);
    }

    public int getLogicalRowGridHeight(int row) {
        return rowOffset[row + 1] - rowOffset[row];
    }

    /**
     *
     */

    public int getLogicalWidth() {
        return columnOffset.length - 1;
    }

    /**
     *
     */

    @Override
    protected ILogicalTable rowsInternal(int from, int to) {
        // ILogicalTable[] rows = getRows();
        //
        // int startHeight = 0;
        // int endHeight = 0;
        // for (int i = 0; i <= to; i++)
        // {
        // int height = rows[i].getGridTable().getGridHeight();
        // if (i < from)
        // startHeight += height;
        // endHeight += height;
        // }

        return LogicalTable.logicalTable(gridTable.rows(rowOffset[from], rowOffset[to + 1] - 1));
    }

    /**
     *
     */

    public ILogicalTable transpose() {
        return LogicalTable.logicalTable(new TransposedGridTable(gridTable));
    }

}
