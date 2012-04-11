package org.openl.rules.table;


/**
 * Logical table model that delegate original table and access to original table
 * through coordinates transformation.
 * 
 * @author PUdalau
 */
// TODO: It have to be AGridTableDelegator except AGridModel.
public class TransformedGridTable extends AGridModel {
    private IGridTable gridTable;
    private CoordinatesTransformer transformer;

    public TransformedGridTable(IGridTable grid, CoordinatesTransformer transformer) {
        this.gridTable = grid;
        this.transformer = transformer;
    }

    /**
     * @return {@link CoordinatesTransformer} of this logical table.
     */
    public CoordinatesTransformer getTransformer() {
        return transformer;
    }

    public ICell getCell(int column, int row) {
        Point point = getCoordinates(column, row);
        return gridTable.getCell(point.getColumn(), point.getRow());
    }

    public int getColumnWidth(int col) {
        Point point = getCoordinates(col, 0);
        return gridTable.getGrid().getColumnWidth(point.getColumn());
    }

    public int getMaxColumnIndex(int row) {
        return transformer.getWidth() - 1;
    }

    public int getMaxRowIndex() {
        return transformer.getHeight() - 1;
    }

    public IGridRegion getMergedRegion(int i) {
        return null;
    }

    public int getMinColumnIndex(int row) {
        return 0;
    }

    public int getMinRowIndex() {
        return 0;
    }

    public int getNumberOfMergedRegions() {
        return 0;
    }

    public String getRangeUri(int colStart, int rowStart, int colEnd, int rowEnd) {
        if (colStart == colEnd && rowStart == rowEnd) {
            return getUri() + "&" + "cell=" + getCell(colStart, rowStart).getUri();
        }

        return getUri() + "&" + "range=" + getCell(colStart, rowStart).getUri() + RANGE_SEPARATOR
            + getCell(colEnd, rowEnd).getUri();
    }

    public String getUri() {
        return gridTable.getGrid().getUri();
    }

    public boolean isEmpty(int col, int row) {
        Point point = getCoordinates(col, row);
        return gridTable.getGrid().isEmpty(point.getColumn(), point.getRow());
    }

    private Point getCoordinates(int col, int row) {
        Point point = transformer.calculateCoordinates(col, row);
        return point;
    }

    public IGridTable asGridTable() {
        return new GridTable(0, 0, getMaxRowIndex(), getMaxColumnIndex(0), this);
    }
}
