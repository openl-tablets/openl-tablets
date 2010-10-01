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
        Point point = getCoordinates(column, row);
        return table.getCell(point.getColumn(), point.getRow());
    }

    @Override
    public String getUri() {
        return table.getGrid().getUri();
    }

    private Point getCoordinates(int col, int row) {
        Point point = transformer.calculateCoordinates(col, row);
        return point;
    }

    public int getWidth() {
        return transformer.getWidth();
    }

    public int getHeight() {        
        return transformer.getHeight();
    }

    public int getGridRow(int column, int row) {
        Point point = getCoordinates(column, row);
        return table.getGridRow(point.getColumn(), point.getRow());
    }

    public int getGridColumn(int column, int row) {        
        Point point = getCoordinates(column, row);
        return table.getGridColumn(point.getColumn(), point.getRow());
    }

    public boolean isNormalOrientation() {        
        return table.isNormalOrientation();
    }

}
