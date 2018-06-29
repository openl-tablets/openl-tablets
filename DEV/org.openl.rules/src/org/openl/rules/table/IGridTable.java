package org.openl.rules.table;

import org.openl.rules.table.xls.XlsUrlParser;

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

    XlsUrlParser getUriParser();

    String getUri(int col, int row);

}
