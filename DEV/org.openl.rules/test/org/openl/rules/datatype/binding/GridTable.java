package org.openl.rules.datatype.binding;

import org.openl.rules.table.*;

/**
 * Default implementation for IGridTable.
 * Is based on the two-dimensional array, the analog of the grid.
 *
 * The purpose of the implementation: use it for test, without creating
 * underlying excel sheet.
 *
 * Supports merged cells horizontally and vertically.
 *
 * Cell is considered to be mergered if there is not empty cell followed by the null cell.
 *
 * @author Denis Levchuk
 */
public class GridTable extends AGridTable {
    private Object[][] values;

    private IGrid grid;

    public GridTable(Object[][] cells) {
        // Flag indicating that previously there was a
        // null cell
        //
        boolean nullCell = false;

        // Flag indicating that previosly there was a
        // not null cell
        //
        boolean notNullCell = false;
        for (int i = 0; i < cells.length; i++) {
            for (int j = 0; j < cells[i].length; j++) {
                if (cells[i][j] != null) {
                    // Found the not null cell
                    //
                    notNullCell = true;

                    // Check if there was a null cell before
                    //
                    if (nullCell) {
                        // Reset the null cell
                        //
                        nullCell = false;
                    }
                }
                if (cells[i][j] == null) {
                    if (!notNullCell) {
                        throw new IllegalArgumentException("There should be any not null value before the null");
                    }
                    nullCell = true;
                }
            }
        }
        this.values = cells;
    }

    @Override
    public int getWidth() {
        return values[0].length;
    }

    @Override
    public int getHeight() {
        return values.length;
    }

    @Override
    public boolean isNormalOrientation() {
        return true;
    }

    @Override
    public ICell getCell(int column, int row) {
        Cell cell = new Cell();
        cell.setColumn(column);
        cell.setRow(row);

        Object value = values[row][column];
        if (value != null) {
            cell.setObjectValue(value);
            cell.setStringValue(value.toString());

            // Set width of the cell
            // 1 is the given cell itself
            // add the number of empty cells to the right (merged cell)
            //
            int rightEmptyCells = rightEmptyCells(column, row);
            cell.setWidth(1 + rightEmptyCells);

            // Set height of the cell
            // 1 is the given cell itself
            // add the number of empty cells down (merged cell)
            //
            int downEmptyCells = downEmptyCells(column, row);
            cell.setHeight(1 + downEmptyCells);
            GridRegion region = new GridRegion(row, column, row + downEmptyCells, column + rightEmptyCells);
            cell.setRegion(region);
        }
        // If value is null, nothing should be set
        // to the cell

        return cell;
    }

    /**
     * Counts the number if the null cells down
     *
     * @param column current column index
     * @param row current row
     * @return number of null cells down after the given one
     */
    private int downEmptyCells(int column, int row) {
        int i = 0;
        while (row + 1 < getHeight()) {
            Object nextRowValue = values[row + 1][column];
            if (nextRowValue == null) {
                i++;
                row++;
            }
            else {
                return i;
            }
        }
        return i;
    }

    /**
     * Counts the number if the null cells right
     *
     * @param column current column index
     * @param row current row
     * @return number of null cells right after the given one
     */
    private int rightEmptyCells(int column, int row) {
        int i = 0;
        while(column + 1 < getWidth()) { // Last Column in the row
            Object next = values[row][column + 1];
            if (next == null) {
                i++;
                column++;
            }
            else {
                return i;
            }

        }
        return i;
    }

    @Override
    public IGrid getGrid() {
        return grid;
    }

    public void setGrid(IGrid grid) {
        this.grid = grid;
    }

    @Override
    public int getGridRow(int column, int row) {
        // Left default implementation
        // TODO: Implement if needed
        return 0;
    }

    @Override
    public int getGridColumn(int column, int row) {
        // Left Default implementation
        // TODO: Implement if needed
        return 0;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
