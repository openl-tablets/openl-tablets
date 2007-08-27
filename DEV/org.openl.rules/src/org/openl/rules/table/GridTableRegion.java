/*
 * Created on Jan 5, 2004
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.rules.table;

/**
 * @author snshor
 *
 */
public class GridTableRegion extends AGridTableDelegator
{

	int column;
	int row;
	int width;
	int height;
	

	/**
	 * @param gridTable
	 */
	public GridTableRegion(IGridTable gridTable,		int column,
	int row,
	int width,
	int height)

	{
		super(gridTable);
		this.column = column;
		this.row = row;
		this.width = width;
		this.height = height;
	}

	/**
	 *
	 */

	public int getGridColumn(int column, int row)
	{
		return gridTable.getGridColumn(this.column + column, this.row + row);
	}

	/**
	 *
	 */

	public int getGridHeight()
	{
		return height;
	}

	/**
	 *
	 */

	public int getGridRow(int column, int row)
	{
		return gridTable.getGridRow(this.column + column, this.row + row);
	}

	/**
	 *
	 */

	public int getGridWidth()
	{
		return width;
	}

	/**
	 *
	 */

	public boolean isNormalOrientation()
	{
		return gridTable.isNormalOrientation();
	}

}
