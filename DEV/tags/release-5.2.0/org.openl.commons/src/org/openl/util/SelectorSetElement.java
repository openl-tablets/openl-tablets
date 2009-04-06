/**
 * Created Apr 24, 2007
 */
package org.openl.util;

/**
 * @author snshor
 * 
 */

public class SelectorSetElement
{
	static public final String OR = "OR", AND = "AND";

	boolean isNot = false;

	String andOr = AND;

	protected ISelector selector;

	public String getAndOr()
	{
		return this.andOr;
	}

	public void setAndOr(String andOr)
	{
		this.andOr = andOr;
	}

	public boolean isNot()
	{
		return this.isNot;
	}

	public void setNot(boolean isNot)
	{
		this.isNot = isNot;
	}

	public ISelector getSelector()
	{
		return this.selector;
	}

	public void setSelector(ISelector selector)
	{
		this.selector = selector;
	}

	boolean select(Object obj)
	{
		return isNot ? !selector.select(obj) : selector.select(obj);
	}

	static public boolean select(Object obj, SelectorSetElement[] elements)
	{
		boolean res = true;

		for (int i = 0; i < elements.length; i++)
		{
			boolean e = elements[i].select(obj);
			if (elements[i].andOr.equals(OR))
			{
				if (res == true && i != 0)
					return true;
				res = e;
			} 
			else
			{	
				res &= e;
			}	
		}
		return res;
	}
}
