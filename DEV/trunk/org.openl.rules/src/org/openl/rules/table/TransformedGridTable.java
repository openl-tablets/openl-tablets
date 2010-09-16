package org.openl.rules.table;


/**
 * Logical table model that delegate original table and access to original table
 * through coordinates transformation.
 * 
 * @author PUdalau
 */
public class TransformedGridTable extends AGridTableDelegator {
    
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

    public ICell getCell(int column, int row) {
        Point point = getCoordinates(column, row);
        return gridTable.getCell(point.getColumn(), point.getRow());
    }
    
    public String getUri() {
        return gridTable.getGrid().getUri();
    }

    private Point getCoordinates(int col, int row) {
        Point point = transformer.calculateCoordinates(col, row);
        return point;
    }

    public int getGridColumn(int column, int row) {        
        Point point = getCoordinates(column, row);
        return gridTable.getGridColumn(point.getColumn(), point.getRow());
    }

    public int getGridHeight() {        
        return transformer.getHeight();
    }

    public int getGridRow(int column, int row) {
        Point point = getCoordinates(column, row);
        return gridTable.getGridRow(point.getColumn(), point.getRow());
    }

    public int getGridWidth() {
        return transformer.getWidth();
    }

    public boolean isNormalOrientation() {        
        return gridTable.isNormalOrientation();
    }
}
