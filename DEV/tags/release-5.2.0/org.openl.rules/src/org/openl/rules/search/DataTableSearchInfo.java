/**
 * Created May 2, 2007
 */
package org.openl.rules.search;

import org.openl.rules.data.ITable;
import org.openl.rules.data.binding.DataTableBoundNode.DataOpenField;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IGridTable;
import org.openl.types.IOpenClass;

/**
 * @author snshor
 *
 */
public class DataTableSearchInfo implements ITableSearchInfo
{
	
	TableSyntaxNode tsn;
	
	public DataTableSearchInfo(TableSyntaxNode tsn)
	{
		this.tsn = tsn;
		DataOpenField df = (DataOpenField)tsn.getMember();
		this.table = df.getTable();
	}
	
	ITable table;

	public int numberOfRows()
	{
		return table.getNumberOfRows();
	}

	public int numberOfColumns()
	{
		return table.getNumberOfColumns();
	}

	public String columnName(int n)
	{
		return table.getColumnName(n);
	}

	public String columnDisplay(int n)
	{
		return table.getColumnDisplay(n);
	}

	public IOpenClass columnType(int n)
	{
		return table.getColumnType(n);
	}

	public Object tableValue(int col, int row)
	{
		return table.getValue(col, row);
	}

	public IGridTable rowTable(int row)
	{
		return table.getRowTable(row);
	}

	public IGridTable headerDisplayTable()
	{
		return table.getHeaderTable();
	}

	public TableSyntaxNode getTableSyntaxNode()
	{
		return tsn;
	}
	
	

}
