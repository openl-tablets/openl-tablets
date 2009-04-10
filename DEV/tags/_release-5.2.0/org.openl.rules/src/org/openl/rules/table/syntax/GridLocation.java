/*
 * Created on Oct 7, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.rules.table.syntax;

import org.openl.rules.table.IGridTable;
import org.openl.util.text.ILocation;
import org.openl.util.text.IPosition;


/**
 * @author snshor
 *
 */
public class GridLocation implements ILocation
{
	GridPosition start, end;
		
		
	public GridLocation(IGridTable table, int x1, int y1, int x2, int y2)
	{
		start = new GridPosition(table.getGridColumn(x1, y1), table.getGridRow(x1, y1), table.getGrid());	
		end = new GridPosition(table.getGridColumn(x2, y2), table.getGridRow(x2, y2), table.getGrid());;
	}


	public GridLocation(IGridTable table, int x1, int y1)
	{
		start = new GridPosition(table.getGridColumn(x1, y1), table.getGridRow(x1, y1), table.getGrid());;	
		end = null;
	}



	public GridLocation(IGridTable table)
	{
		start = new GridPosition(table.getGridColumn(0, 0), table.getGridRow(0, 0), table.getGrid());
		int w = table.getGridWidth();
		int h = table.getGridHeight();
			
		end = new GridPosition(table.getGridColumn(w-1, h-1), table.getGridRow(w-1, h-1), table.getGrid());
	}
	
	

	/**
	 * @return
	 */
	public IPosition getEnd()
	{
		return end;
	}

	/**
	 * @return
	 */
	public IPosition getStart()
	{
		return start;
	}

	public String toString()
	{
		if (end == null)
		  return XlsURLConstants.CELL + "="+start;
		return XlsURLConstants.RANGE +  "=" + start + ":" + end;  
	}


	/**
	 *
	 */

	public boolean isTextLocation()
	{
		return false;
	}

}
