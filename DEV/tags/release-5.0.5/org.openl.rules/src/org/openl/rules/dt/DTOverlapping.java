/**
 * Created Feb 11, 2007
 */
package org.openl.rules.dt;

import org.openl.util.ArrayOfNamedValues;

/**
 * @author snshor
 *
 */
public class DTOverlapping
{
	int[] rules;
	
	ArrayOfNamedValues value;

	public DTOverlapping(int[] rules, ArrayOfNamedValues value)
	{
		this.rules = rules;
		this.value = value;
	}

	public int[] getRules()
	{
		return this.rules;
	}

	public ArrayOfNamedValues getValue()
	{
		return this.value;
	}
	
}
