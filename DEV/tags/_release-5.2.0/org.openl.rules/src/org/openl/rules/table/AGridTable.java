/*
 * Created on Oct 28, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.table;

import org.openl.rules.table.ui.ICellStyle;


/**
 * @author snshor
 *
 */
public abstract class AGridTable extends ALogicalTable implements IGridTable
{

	/**
	 *
	 */

	public int getCellHeight(int column, int row)
	{
		return isNormalOrientation()
			? getGrid().getCellHeight(
				getGridColumn(column, row),
				getGridRow(column, row))
			: getGrid().getCellWidth(
				getGridColumn(column, row),
				getGridRow(column, row));
	}
	
	
	public boolean isPartOfTheMergedRegion(int column, int row)
	{
		return getGrid().isPartOfTheMergedRegion(getGridColumn(column, row), getGridRow(column, row));
	}
	

	public ICellInfo getCellInfo(int column, int row)
	{
		return isNormalOrientation()
		? getGrid().getCellInfo(
			getGridColumn(column, row),
			getGridRow(column, row))
		: getGrid().getCellInfo(
			getGridColumn(column, row),
			getGridRow(column, row));
	}


	/**
	 *
	 */

	public int getCellWidth(int column, int row)
	{
		return isNormalOrientation()
			? getGrid().getCellWidth(
				getGridColumn(column, row),
				getGridRow(column, row))
			: getGrid().getCellHeight(
				getGridColumn(column, row),
				getGridRow(column, row));
	}

	/**
	 *
	 */

	protected ILogicalTable columnsInternal(int from, int to)
	{
		return new GridTableColumns(this, from, to);
	}

	/**
	 *
	 */

	public IGridTable getGridTable()
	{
		return this;
	}
	
	
	
	

	public IGridRegion getRegion()
	{
		int left = getGridColumn(0, 0);
		int top = getGridRow(0, 0);
		
		
		
		int right = -1; 
		int bottom = -1;
		
		if (isNormalOrientation())
		{
			right = getGridColumn(getGridWidth() - 1, 0);
			bottom = getGridRow(0, getGridHeight()-1);
		}
		else
		{
			right = getGridColumn(0, getGridHeight() - 1);
			bottom = getGridRow(getGridWidth() - 1, 0);
		}	
		
		
		return new GridTable(top, left, bottom, right, getGrid());
	}


	/**
	 *
	 */

	public int getLogicalHeight()
	{
		return getGridHeight();
	}

	/**
	 *
	 */

	public int getLogicalWidth()
	{
		return getGridWidth();
	}

	/**
	 *
	 */

	protected ILogicalTable rowsInternal(int from, int to)
	{
		return new GridTableRows(this, from, to);
	}

	/**
	 *
	 */

	public ILogicalTable transpose()
	{
		return new TransposedGridTable(this);
	}

	/**
	 *
	 */

	public String getStringValue(int col, int row)
	{
		return getGrid().getStringCellValue(
			getGridColumn(col, row),
			getGridRow(col, row));
	}

	public Object getObjectValue(int col, int row)
	{
		return getGrid().getObjectCellValue(
				getGridColumn(col, row),
				getGridRow(col, row));
	}


	/**
	 *
	 */

	public String getUri(int col, int row)
	{
		int colStart = getGridColumn(col, row);
		int rowStart = getGridRow(col, row);
		return getGrid().getRangeUri(colStart, rowStart, colStart, rowStart);
	}


	public String getUri()
	{
		int w = getGridWidth();
		int h = getGridHeight();
		return getGrid().getRangeUri(getGridColumn(0, 0), getGridRow(0, 0),
					getGridColumn(w -1, h - 1), getGridRow(w - 1, h - 1));
	}


	/**
	 *
	 */

	public ILogicalTable getLogicalColumn(int column)
	{
		return columns(column, column);
	}

	/**
	 *
	 */

	public ILogicalTable getLogicalRow(int row)
	{
		return rows(row, row);
	}

	/**
	 *
	 */

	protected ILogicalTable getLogicalRegionInternal(
		int column,
		int row,
		int width,
		int height)
	{
		return new GridTableRegion(this, column, row, width, height);
	}

	/**
	 *
	 */

	public ICellStyle getCellStyle(int col, int row)
	{
		return getGrid().getCellStyle(
			getGridColumn(col, row),
			getGridRow(col, row));
	}


	public int getLogicalColumnGridWidth(int column)
	{
		return 1;
	}


	public int getLogicalRowGridHeight(int row)
	{
		return 1;
	}


	public int findColumnStart(int gridOffset) throws TableException
	{
		if (gridOffset < getLogicalWidth())
			return gridOffset;
		throw new TableException("gridOffset is higher than table's width");
	}


	public int findRowStart(int gridOffset) throws TableException
	{
		if (gridOffset < getLogicalHeight())
			return gridOffset;
		throw new TableException("gridOffset is higher than table's height");
	}

}
