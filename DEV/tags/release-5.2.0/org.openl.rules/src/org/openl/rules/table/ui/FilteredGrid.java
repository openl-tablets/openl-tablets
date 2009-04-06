/**
 * Created Feb 27, 2007
 */
package org.openl.rules.table.ui;

import java.util.HashMap;

import org.openl.rules.table.CellKey;
import org.openl.rules.table.GridDelegator;
import org.openl.rules.table.ICellInfo;
import org.openl.rules.table.IGrid;

/**
 * @author snshor
 *
 */
public class FilteredGrid extends GridDelegator
{

	IGridFilter[] formatFilters;
	
	HashMap<CellKey, FormattedCell> formattedCells = new HashMap<CellKey, FormattedCell>();
	
	public synchronized FormattedCell getFormattedCell(int col, int row)
	{
		CellKey ckey = new CellKey(col, row);
		FormattedCell fcell = formattedCells.get(ckey);
		if (fcell == null)
		{
			fcell = new FormattedCell(delegate.getCellInfo(col, row));
			formatCell(fcell,  col, row);
			formattedCells.put(ckey, fcell);
		}
		return fcell;
	}
	
	
	public ICellInfo getCellInfo(int column, int row)
	{
		if (isEmpty(column, row))
			super.getCellInfo(column, row);
		
		return getFormattedCell(column, row);
	}


	/**
	 * @param fcell
	 * @param col
	 * @param row
	 */
	private void formatCell(FormattedCell fcell, int col, int row)
	{
		fcell.content = getStringCellValue(col, row);
		fcell.type = getCellType(col, row);
		fcell.value = getObjectCellValue(col, row);
		
		
		if (formatFilters != null)
		{
			for (int i = 0; i < formatFilters.length; i++)
			{
				if (formatFilters[i].getGridSelector() == null || formatFilters[i].getGridSelector().selectCoords(col, row))
				{
					fcell = formatFilters[i].filterFormat(fcell);
				}	
			}
		}	
	}


	public String getFormattedCellValue(int column, int row)
	{
		if (isEmpty(column, row))
			return null; 

		
		FormattedCell fcell = getFormattedCell(column, row);		
		
		return fcell.content;
	}

//	public FormattedCell getFormattedCell(int column, int row)
//	{
//		if (isEmpty(column, row))
//			return null; 
//
//		
//		FormattedCell fcell = getFormattedCell(column, row);		
//		
//		return fcell;
//	}
	

	/**
	 * @param delegate
	 */
	public FilteredGrid(IGrid delegate, IGridFilter[] formatFilters)
	{
		super(delegate);
		this.formatFilters = formatFilters;
	}
	
	
	
	
	

	
}
