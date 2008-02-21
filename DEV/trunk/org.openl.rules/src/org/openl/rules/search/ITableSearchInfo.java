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
public interface ITableSearchInfo
{
	
	TableSyntaxNode getTableSyntaxNode();
	
	int numberOfRows();
	int numberOfColumns();
	
	String columnName(int n);
	String columnDisplay(int n);
	IOpenClass columnType(int n);
	
	Object tableValue(int col, int row);
	
	IGridTable rowTable(int row);
	IGridTable headerDisplayTable();
	
	
	
}
