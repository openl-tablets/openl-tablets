package org.openl.rules.table;

/**
 * TODO: create {@link CoordinatesTransformer} for this case of transposed grid table. 
 * 
 * @author snshor
 */
public class TransposedGridTable extends AGridTableDecorator {
    
    private boolean isNormalOrientation;
    
    public TransposedGridTable(IGridTable gridTable) {
        super(gridTable);
        this.isNormalOrientation = !table.isNormalOrientation();
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
        return isNormalOrientation;
    }

    @Override
    public IGridTable transpose() {
        return table;
    }

}
