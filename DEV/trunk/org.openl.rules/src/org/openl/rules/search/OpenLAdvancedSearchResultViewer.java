/**
 * Created May 14, 2007
 */
package org.openl.rules.search;

import org.openl.rules.table.CompositeGrid;
import org.openl.rules.table.IGridRegion;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.TransposedGridTable;

/**
 * @author snshor
 *
 */
public class OpenLAdvancedSearchResultViewer
{

	
	OpenLAdvancedSearchResult result;

	public OpenLAdvancedSearchResultViewer(OpenLAdvancedSearchResult result)
	{
		this.result = result;
	}
	
	
	public CompositeGrid makeGrid(ISearchTableRow[] rows)
	{
	
		if (rows.length == 0)
			return null;
		
		IGridTable[] tables = new IGridTable[rows.length+1];
		
		tables[0] = rows[0].getTableSearchInfo().headerDisplayTable();
		
//		boolean isVertical = rows[0].getRowTable().isNormalOrientation();
		
		boolean isVertical = isVertical(rows[0].getRowTable());

		
		for (int i = 0; i < rows.length; i++)
		{
			tables[1+i] = align(rows[i].getRowTable(), isVertical);
		}
		
		
		return new CompositeGrid(tables, isVertical);
		
	}
	
	
	
	private IGridTable align(IGridTable rowTable, boolean isVertical)
	{
		return isVertical == isVertical(rowTable) ? rowTable : new TransposedGridTable(rowTable);
	}


	boolean isVertical(IGridTable t)
	{
		 return IGridRegion.Tool.width(t.getRegion()) >= IGridRegion.Tool.height(t.getRegion());
	}
	
	

	
	
	
}
