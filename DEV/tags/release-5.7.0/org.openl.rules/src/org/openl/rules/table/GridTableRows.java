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
public class GridTableRows extends AGridTableDelegator {

    private int fromRow;
    private int toRow;

    public GridTableRows(IGridTable gridTable, int fromRow) {
        this(gridTable, fromRow, gridTable.getGridHeight() - 1);
    }

    public GridTableRows(IGridTable gridTable, int fromRow, int toRow) {
        super(gridTable);
        if (fromRow - toRow > 1) {
            throw new ArrayIndexOutOfBoundsException(toRow);
        }

        this.fromRow = fromRow;
        this.toRow = toRow;
    }

    public int getGridColumn(int column, int row) {
        return gridTable.getGridColumn(column, row + fromRow);
    }

    public int getGridHeight() {
        return toRow - fromRow + 1;
    }

    public int getGridRow(int column, int row) {
        return gridTable.getGridRow(column, row + fromRow);
    }

    public int getGridWidth() {
        return gridTable.getGridWidth();
    }

    public boolean isNormalOrientation() {
        return gridTable.isNormalOrientation();
    }

    @Override
    protected ILogicalTable rowsInternal(int from, int to) {
        return new GridTableRows(gridTable, fromRow + from, fromRow + to);
    }

}
