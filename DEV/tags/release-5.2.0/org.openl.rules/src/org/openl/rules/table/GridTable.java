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
public class GridTable extends AGridTable implements IGridRegion
{

	IGrid grid;

	int top, left, right, bottom;

	public GridTable(int top, int left, int bottom, int right, IGrid grid)
	{
		this.top = top;
		this.left = left;
		this.bottom = bottom;
		this.right = right;
		this.grid = grid;
	}

	

	public GridTable(IGridRegion reg, IGrid grid)
	{	
		this.top = reg.getTop();
		this.left = reg.getLeft();
		this.bottom = reg.getBottom();
		this.right = reg.getRight();
		this.grid = grid;
	}
	
	
	/**
	 *
	 */

	public IGrid getGrid()
	{
		return grid;
	}

	/**
	 *
	 */

	public int getGridHeight()
	{
		return bottom - top + 1;
	}

	/**
	 *
	 */

	public int getGridWidth()
	{
		return right - left + 1;
	}

	/**
	 *
	 */

	public int getGridColumn(int column, int row)
	{
		return left + column;
	}

	/**
	 *
	 */

	public int getGridRow(int column, int row)
	{
		return top + row;
	}

	/**
	 *
	 */

	public boolean isNormalOrientation()
	{
		return true;
	}

	/**
	 * @return
	 */
	public int getBottom()
	{
		return bottom;
	}

	/**
	 * @return
	 */
	public int getLeft()
	{
		return left;
	}

	/**
	 * @return
	 */
	public int getRight()
	{
		return right;
	}

	/**
	 * @return
	 */
	public int getTop()
	{
		return top;
	}

	public IGridRegion getRegion()
	{
		return this;
	}
	
}
