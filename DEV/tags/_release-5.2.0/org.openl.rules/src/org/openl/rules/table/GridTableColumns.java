/*
 * Created on Oct 28, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.rules.table;

/**
 * @author snshor
 *
 */
public class GridTableColumns extends AGridTableDelegator
{
	
	int fromColumn; 
	int toColumn;

	/**
	 * @param gridTable
	 */
	public GridTableColumns(IGridTable gridTable, int fromColumn, int toColumn)
	{
		super(gridTable);
		this.fromColumn = fromColumn;
		this.toColumn = toColumn;
	}

	public GridTableColumns(IGridTable gridTable, int fromColumn)
	{
		this(gridTable, fromColumn, gridTable.getGridWidth() - 1);
	}



	/**
	 *
	 */

	public int getGridHeight()
	{
		return gridTable.getGridHeight();
	}

	/**
	 *
	 */

	public int getGridWidth()
	{
		return toColumn - fromColumn + 1;
	}

	/**
	 *
	 */

	public int getGridColumn(int column, int row)
	{
		return gridTable.getGridColumn(column + fromColumn, row);
	}

	/**
	 *
	 */

	public int getGridRow(int column, int row)
	{
		return gridTable.getGridRow(column + fromColumn, row);
	}

	/**
	 *
	 */

	public boolean isNormalOrientation()
	{
		return gridTable.isNormalOrientation();
	}

	/**
	 *
	 */

	protected ILogicalTable columnsInternal(int from, int to)
	{
		return new GridTableColumns(gridTable, fromColumn + from, fromColumn + to);
	}

}
