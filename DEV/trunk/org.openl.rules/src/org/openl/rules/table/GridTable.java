package org.openl.rules.table;

/**
 * @author snshor
 *
 */
public class GridTable extends AGridTable {

    private IGridRegion region;
    private IGrid grid;
    private int width;
    private int height;

    public GridTable(IGridRegion reg, IGrid grid) {
        this.region = reg;
        this.grid = grid;
        this.width = region.getRight() - region.getLeft() + 1;
        this.height = region.getBottom() - region.getTop() + 1;
    }

    public GridTable(int top, int left, int bottom, int right, IGrid grid) {
        this(new GridRegion(top, left, bottom, right), grid);
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public IGrid getGrid() {
        return grid;
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
