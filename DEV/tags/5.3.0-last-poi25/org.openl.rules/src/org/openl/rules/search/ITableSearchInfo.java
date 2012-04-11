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

    String columnDisplay(int n);

    String columnName(int n);

    IOpenClass columnType(int n);

    TableSyntaxNode getTableSyntaxNode();

    IGridTable headerDisplayTable();

    int numberOfColumns();

    int numberOfRows();

    IGridTable rowTable(int row);

    Object tableValue(int col, int row);

}
