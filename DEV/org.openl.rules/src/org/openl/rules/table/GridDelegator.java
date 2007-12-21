/**
 * Created Feb 27, 2007
 */
package org.openl.rules.table;

import java.util.Date;

import org.openl.rules.table.ui.FormattedCell;
import org.openl.rules.table.ui.ICellStyle;

/**
 * @author snshor
 *
 */

public class GridDelegator implements IGrid
{

	protected IGrid delegate;

	public GridDelegator(IGrid delegate)
	{
		this.delegate = delegate;
	}

	public int getCellHeight(int column, int row)
	{
		return this.delegate.getCellHeight(column, row);
	}

	public ICellInfo getCellInfo(int column, int row)
	{
		return this.delegate.getCellInfo(column, row);
	}

	public ICellStyle getCellStyle(int column, int row)
	{
		return this.delegate.getCellStyle(column, row);
	}

	public int getCellType(int column, int row)
	{
		return this.delegate.getCellType(column, row);
	}

	public String getCellUri(int column, int row)
	{
		return this.delegate.getCellUri(column, row);
	}

	public int getCellWidth(int column, int row)
	{
		return this.delegate.getCellWidth(column, row);
	}

	public Date getDateCellValue(int column, int row)
	{
		return this.delegate.getDateCellValue(column, row);
	}

	public double getDoubleCellValue(int column, int row)
	{
		return this.delegate.getDoubleCellValue(column, row);
	}

	public int getMaxColumnIndex(int row)
	{
		return this.delegate.getMaxColumnIndex(row);
	}

	public int getMaxRowIndex()
	{
		return this.delegate.getMaxRowIndex();
	}

	public IGridRegion getMergedRegion(int i)
	{
		return this.delegate.getMergedRegion(i);
	}

	public int getMinColumnIndex(int row)
	{
		return this.delegate.getMinColumnIndex(row);
	}

	public int getMinRowIndex()
	{
		return this.delegate.getMinRowIndex();
	}

	public int getNumberOfMergedRegions()
	{
		return this.delegate.getNumberOfMergedRegions();
	}

	public Object getObjectCellValue(int column, int row)
	{
		return this.delegate.getObjectCellValue(column, row);
	}

	public String getRangeUri(int colStart, int rowStart, int colEnd, int rowEnd)
	{
		return this.delegate.getRangeUri(colStart, rowStart, colEnd, rowEnd);
	}

	public IGridRegion getRegionStartingAt(int colFrom, int rowFrom)
	{
		return this.delegate.getRegionStartingAt(colFrom, rowFrom);
	}

	public String getStringCellValue(int column, int row)
	{
		return this.delegate.getStringCellValue(column, row);
	}

	public String getUri()
	{
		return this.delegate.getUri();
	}

	public boolean isEmpty(int col, int row)
	{
		return this.delegate.isEmpty(col, row);
	}

	public boolean isPartOfTheMergedRegion(int col, int row)
	{
		return this.delegate.isPartOfTheMergedRegion(col, row);
	}

	public String getFormattedCellValue(int column, int row)
	{
		return this.delegate.getFormattedCellValue(column, row);
	}

	public int getColumnWidth(int col)
	{
		return this.delegate.getColumnWidth(col);
	}

	public FormattedCell getFormattedCell(int column, int row)
	{
		return delegate.getFormattedCell(column, row);
	}
	
	
	
}
