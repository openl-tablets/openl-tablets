package org.openl.rules.table.ui;

import org.openl.rules.table.IGrid;
import org.openl.rules.table.IGridTable;

public class TableValueFilter extends AGridFilter
{

	Model model;

	int startX, startY;

	IGrid grid;

	public TableValueFilter(IGridTable t, Model m)
	{
		model = m;
		startX = t.getGridColumn(0, 0);
		startY = t.getGridRow(0, 0);
		grid = t.getGrid();
	}

	static public interface Model
	{
		public Object getValue(int col, int row);
	}

	public FormattedCell filterFormat(FormattedCell cell)
	{
		Object v = getCellValue(cell.getColumn(), cell.getRow());

		if (v != null)
		{
			cell.value = v;
			cell.content = String.valueOf(v);
		}	
		return cell;
	}

	private Object getCellValue(int column, int row)
	{
		Object v = model.getValue(column - startX, row - startY);

		return v;
	}

}
