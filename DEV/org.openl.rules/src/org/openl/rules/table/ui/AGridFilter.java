/**
 * Created Mar 1, 2007
 */
package org.openl.rules.table.ui;

public abstract class AGridFilter implements IGridFilter
{
	IGridSelector selector;
	
	public AGridFilter(IGridSelector selector)
	{
		this.selector = selector;
	}

	public AGridFilter()
	{
	}
	
	public IGridSelector getGridSelector()
	{
		return selector;
	}
}