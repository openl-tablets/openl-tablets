package org.openl.rules.table;

/**
 * @author snshor
 *
 */
public class GridTable extends AGridTable {

    private IGridRegion region;
    private IGrid grid;

    public GridTable(IGridRegion reg, IGrid grid) {
        this.region = reg;
        this.grid = grid;
    }

    public GridTable(int top, int left, int bottom, int right, IGrid grid) {
        this(new GridRegion(top, left, bottom, right), grid);
    }

    public int getWidth() {
        return region.getRight() - region.getLeft() + 1;
    }

    public int getHeight() {
        return region.getBottom() - region.getTop() + 1;
    }

    public IGrid getGrid() {
        return grid;
    }

    @Override
    public void edit() {
        // Do nothing
    }

    @Override
    public void stopEditing() {
        // Do nothing
    }

    public int getGridColumn(int column, int row) {
        return region.getLeft() + column;
    }

    public int getGridRow(int column, int row) {
        return region.getTop() + row;
    }

    @Override
    public IGridRegion getRegion() {
        return region;
    }

    public boolean isNormalOrientation() {
        return true;
    }

}
