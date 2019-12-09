package org.openl.rules.table;

/**
 * TODO: create {@link CoordinatesTransformer} for this case of transposed grid table.
 *
 * @author snshor
 */
public class TransposedGridTable extends AGridTableDecorator {

    public TransposedGridTable(IGridTable gridTable) {
        super(gridTable);
    }

    @Override
    public int getGridColumn(int column, int row) {
        return table.getGridColumn(row, column);
    }

    @Override
    public int getHeight() {
        return table.getWidth();
    }

    @Override
    public int getGridRow(int column, int row) {
        return table.getGridRow(row, column);
    }

    @Override
    public int getWidth() {
        return table.getHeight();
    }

    @Override
    public boolean isNormalOrientation() {
        return !table.isNormalOrientation();
    }

    @Override
    public IGridRegion getRegion() {
        int left = table.getRegion().getTop();
        int top = table.getRegion().getLeft();

        int right = table.getRegion().getBottom();
        int bottom = table.getRegion().getRight();

        return new GridRegion(top, left, bottom, right);
    }

    @Override
    public IGridTable transpose() {
        return table;
    }

}
