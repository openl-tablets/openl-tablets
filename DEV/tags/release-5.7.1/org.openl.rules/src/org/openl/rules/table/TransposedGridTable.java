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
public class TransposedGridTable extends AGridTableDelegator {

    public TransposedGridTable(IGridTable gridTable) {
        super(gridTable);
    }

    public int getGridColumn(int column, int row) {
        return gridTable.getGridColumn(row, column);
    }

    public int getGridHeight() {
        return gridTable.getGridWidth();
    }

    public int getGridRow(int column, int row) {
        return gridTable.getGridRow(row, column);
    }

    public int getGridWidth() {
        return gridTable.getGridHeight();
    }

    public boolean isNormalOrientation() {
        return !gridTable.isNormalOrientation();
    }

    @Override
    public ILogicalTable transpose() {
        return gridTable;
    }

}
