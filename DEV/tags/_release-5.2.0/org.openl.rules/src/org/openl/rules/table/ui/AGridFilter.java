/**
 * Created Mar 1, 2007
 */
package org.openl.rules.table.ui;

public abstract class AGridFilter implements IGridFilter
{
	public Object parse(String value)
	{
		throw new UnsupportedOperationException("This format does not parse");
	}

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