package org.openl.rules.table;

/**
 * Logical table model that delegate original table and access to original table
 * through coordinates transformation.
 * 
 * @author PUdalau
 */
public class TransformedGridTable extends AGridTableDecorator {

    private CoordinatesTransformer transformer;

    public TransformedGridTable(IGridTable gridTable, CoordinatesTransformer transformer) {
        super(gridTable);
        this.transformer = transformer;
    }

    /**
     * @return {@link CoordinatesTransformer} of this logical table.
     */
    public CoordinatesTransformer getTransformer() {
        return transformer;
    }

    @Override
    public ICell getCell(int column, int row) {
        return table.getCell(getColumn(column, row), getRow(column, row));
    }

    @Override
    public String getUri() {
        return table.getGrid().getUri();
    }

    private int getRow(int col, int row) {
        return transformer.getRow(col, row);
    }

    private int getColumn(int col, int row) {
        return transformer.getColumn(col, row);
    }

    @Override
    public int getWidth() {
        return transformer.getWidth();
    }

    @Override
    public int getHeight() {
        return transformer.getHeight();
    }

    @Override
    public int getGridRow(int column, int row) {
        return table.getGridRow(getColumn(column, row), getRow(column, row));
    }

    @Override
    public int getGridColumn(int column, int row) {
        return table.getGridColumn(getColumn(column, row), getRow(column, row));
    }

    @Override
    public boolean isNormalOrientation() {
        return table.isNormalOrientation();
    }

}
