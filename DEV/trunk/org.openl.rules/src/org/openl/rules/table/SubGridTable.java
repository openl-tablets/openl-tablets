package org.openl.rules.table;

/**
 * @author snshor
 */
public class SubGridTable extends AGridTableDecorator {

    private int fromColumn;
    private int fromRow;
    private int width;
    private int height;

    public SubGridTable(IGridTable table, int fromColumn, int fromRow, int width, int height) {
        super(table);
        this.fromColumn = fromColumn;
        this.fromRow = fromRow;
        this.width = width;
        this.height = height;
    }

    public int getGridColumn(int col, int row) {
        return table.getGridColumn(fromColumn + col, fromRow + row);
    }

    public int getGridRow(int col, int row) {
        return table.getGridRow(fromColumn + col, fromRow + row);
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

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
