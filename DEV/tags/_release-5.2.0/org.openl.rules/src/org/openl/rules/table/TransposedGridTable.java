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
public class TransposedGridTable extends AGridTableDelegator
{

	/**
	 * @param gridTable
	 */
	public TransposedGridTable(IGridTable gridTable)
	{
		super(gridTable);
	}

	/**
	 *
	 */

	public int getGridHeight()
	{
		return gridTable.getGridWidth();
	}

	/**
	 *
	 */

	public int getGridWidth()
	{
		return gridTable.getGridHeight();
	}

	/**
	 *
	 */

	public int getGridColumn(int column, int row)
	{
		return gridTable.getGridColumn(row, column);
	}

	/**
	 *
	 */

	public int getGridRow(int column, int row)
	{
		return gridTable.getGridRow(row, column);
	}

	/**
	 *
	 */

	public boolean isNormalOrientation()
	{
		return !gridTable.isNormalOrientation();
	}
	
	

	/**
	 *
	 */

	public ILogicalTable transpose()
	{
		return gridTable;
	}

}
