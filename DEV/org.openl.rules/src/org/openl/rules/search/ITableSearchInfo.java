/**
 * Created Apr 29, 2007
 */
package org.openl.rules.search;

import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IGridTable;
import org.openl.types.IOpenClass;

/**
 * @author snshor
 *
 */
public interface ITableSearchInfo {

    String getColumnDisplay(int col);

    String getColumnName(int col);
    
    IOpenClass getColumnType(int col);

    TableSyntaxNode getTableSyntaxNode();

    IGridTable getHeader();

    int getNumberOfColumns();

    int getNumberOfRows();

    IGridTable getRowTable(int row);

    Object getTableValue(int col, int row);

}
