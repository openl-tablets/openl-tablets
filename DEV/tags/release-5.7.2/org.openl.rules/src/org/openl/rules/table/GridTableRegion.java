/*
 * Created on Jan 5, 2004
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.table;

/**
 * @author snshor
 *
 */
public class GridTableRegion extends AGridTableDelegator {

    private int column;
    private int row;
    private int width;
    private int height;

    public GridTableRegion(IGridTable gridTable, int column, int row, int width, int height) {
        super(gridTable);
        this.column = column;
        this.row = row;
        this.width = width;
        this.height = height;
    }

    public int getGridColumn(int xcol, int yrow) {
        return gridTable.getGridColumn(column + xcol, row + yrow);
    }

    public int getGridHeight() {
        return height;
    }

    public int getGridRow(int xcol, int yrow) {
        return gridTable.getGridRow(column + xcol, row + yrow);
    }

    public int getGridWidth() {
        return width;
    }

    public boolean isNormalOrientation() {
        return gridTable.isNormalOrientation();
    }

}
