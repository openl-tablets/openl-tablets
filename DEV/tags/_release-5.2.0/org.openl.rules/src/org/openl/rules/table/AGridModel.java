package org.openl.rules.table;



public abstract class AGridModel implements IGrid
{
	
	public IGridRegion getGridRegionContaining(int x, int y)
	{
		int nregions = getNumberOfMergedRegions();
		for (int i = 0; i < nregions; i++)
		{
			IGridRegion reg = getMergedRegion(i);
			if (IGridRegion.Tool.contains(reg, x, y))
				return reg;
		}
		return null;
	}
	
	public boolean isPartOfTheMergedRegion(int x, int y)
	{
		return getGridRegionContaining(x, y) != null;
	}
	
	public IGridRegion getRegionStartingAt(int colFrom, int rowFrom)
	{
		
		int nregions = getNumberOfMergedRegions();
		for (int i = 0; i < nregions; i++)
		{
			IGridRegion reg = getMergedRegion(i);
			if (reg.getLeft() == colFrom && reg.getTop() == rowFrom)
				return reg;
		}
		return null;

	}
	
	

}
