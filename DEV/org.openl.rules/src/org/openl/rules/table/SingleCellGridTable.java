package org.openl.rules.table;

/**
 * Single cell grid table. Introduced to optimize SubGridTable
 *
 * @author snshor
 */
public class SingleCellGridTable extends AGridTableDecorator {

    private int fromColumn;
    private int fromRow;

    public SingleCellGridTable(IGridTable table, int fromColumn, int fromRow) {
        super(table);
        this.fromColumn = fromColumn;
        this.fromRow = fromRow;
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
        return 1;
    }

    @Override
    public int getWidth() {
        return 1;
    }

    @Override
    public boolean isNormalOrientation() {
        return table.isNormalOrientation();
    }

    @Override
    public IGridTable getSubtable(int column, int row, int width, int height) {
        return this;
    }

    @Override
    public ICell getCell(int column, int row) {
        if (cachedCell == null) {
            cachedCell = table.getCell(fromColumn, fromRow);
        }
        return cachedCell;
    }

    ICell cachedCell = null;

}
