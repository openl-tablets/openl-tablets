package org.openl.rules.table;

/**
 * Part of the grid table. Allows to get different parts from the table.
 *
 * @author snshor
 */
public class SubGridTable extends AGridTableDecorator {

    private final int fromColumn;
    private final int fromRow;
    private final int width;
    private final int height;
    private final IGridRegion region;

    public SubGridTable(IGridTable table, int fromColumn, int fromRow, int width, int height) {
        super(table);
        this.fromColumn = fromColumn;
        this.fromRow = fromRow;
        this.width = width;
        this.height = height;
        this.region = super.getRegion();
    }

    @Override
    public IGridRegion getRegion() {
        return region;
    }

    @Override
    public int getGridColumn(int col, int row) {
        return table.getGridColumn(fromColumn + col, fromRow + row);
    }

    @Override
    public int getGridRow(int col, int row) {
        return table.getGridRow(fromColumn + col, fromRow + row);
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public boolean isNormalOrientation() {
        return table.isNormalOrientation();
    }

    @Override
    public IGridTable getSubtable(int column, int row, int width, int height) {
        return table.getSubtable(fromColumn + column, fromRow + row, width, height);
    }

    @Override
    public ICell getCell(int column, int row) {
        return table.getCell(fromColumn + column, fromRow + row);
    }

}
