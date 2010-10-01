package org.openl.rules.table;

/**
 * @author snshor
 */
public class TransposedGridTable extends AGridTableDecorator {

    public TransposedGridTable(IGridTable gridTable) {
        super(gridTable);
    }

    public int getGridColumn(int column, int row) {
        return table.getGridColumn(row, column);
    }

    public int getHeight() {
        return table.getWidth();
    }

    public int getGridRow(int column, int row) {
        return table.getGridRow(row, column);
    }

    public int getWidth() {
        return table.getHeight();
    }

    public boolean isNormalOrientation() {
        return !table.isNormalOrientation();
    }

    @Override
    public IGridTable transpose() {
        return table;
    }

}
