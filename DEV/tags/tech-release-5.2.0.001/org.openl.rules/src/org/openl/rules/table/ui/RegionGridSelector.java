/**
 * Created Mar 1, 2007
 */
package org.openl.rules.table.ui;

import org.openl.rules.table.IGridRegion;

/**
 * @author snshor
 *
 */
public class RegionGridSelector implements IGridSelector
{
	
	IGridRegion[] regions;
	boolean exclude;
	
	
	public RegionGridSelector(IGridRegion region, boolean exclude) 
	{
		this.regions = new IGridRegion[]{region};
		this.exclude = exclude;
	}
	
	public RegionGridSelector(IGridRegion[] regions, boolean exclude) 
	{
		this.regions = regions;
		this.exclude = exclude;
	}


	

	public boolean selectCoords(int col, int row)
	{
		return contains(col, row) ^ exclude;
	}
	
	
	boolean contains(int col, int row)
	{
		for (int i = 0; i < regions.length; i++)
		{
			if (IGridRegion.Tool.contains(regions[i], col, row))
				return true;
		}
		return false;
	}

}
