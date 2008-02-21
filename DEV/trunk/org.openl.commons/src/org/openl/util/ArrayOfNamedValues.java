/**
 * Created Feb 11, 2007
 */
package org.openl.util;

/**
 * @author snshor
 */
public class ArrayOfNamedValues
{
	
	String[] names;
	Object[] values;
	
	public ArrayOfNamedValues(String[] names, Object[] values)
	{
		this.names = names;
		this.values = values;
	}


	public int size()
	{
		return names.length;
	}
	
	
	public String getName(int i)
	{
		return names[i];
	}

	public Object getValue(int i)
	{
		return values[i];
	}
	
}
