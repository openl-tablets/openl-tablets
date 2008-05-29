/**
 * Created Feb 11, 2007
 */
package org.openl.rules.dt;

import org.openl.util.ArrayOfNamedValues;

/**
 * @author snshor
 *
 */
public class DTUncovered
{
	
	ArrayOfNamedValues values;

	/**
	 * @param values 
	 * 
	 */
	public DTUncovered(ArrayOfNamedValues values)
	{
		this.values = values;
	}

	public ArrayOfNamedValues getValues()
	{
		return this.values;
	}

	public String toString() 
	{
		return values.toString();
	}

}
