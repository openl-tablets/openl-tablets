package org.openl.rules.table;

/**
 * Table based on Grid coordinates.
 * 
 * @author snshor
 *
 */
public interface IGridTable extends ITable<IGridTable> {

    IGrid getGrid();

    void edit();

    void stopEditing();

    int getGridRow(int column, int row);

    int getGridColumn(int column, int row);

    IGridRegion getRegion();

    String getUri();

    String getUri(int col, int row);

}
