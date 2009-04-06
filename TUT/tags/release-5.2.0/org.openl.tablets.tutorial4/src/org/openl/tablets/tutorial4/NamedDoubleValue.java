/**
 * Created Feb 1, 2007
 */
package org.openl.tablets.tutorial4;

import org.openl.base.NamedThing;
import org.openl.meta.DoubleValue;
import org.openl.meta.DoubleValueFunction;

/**
 * @author snshor
 *
 */
public class NamedDoubleValue extends NamedThing
{

	DoubleValue result;
	
	
	public NamedDoubleValue(String name, DoubleValue result)
	{
		super(name);
		this.result = result;
	}

	/**
	 * 
	 */
	public NamedDoubleValue()
	{
		super();
	}
	
	public DoubleValue getResult()
	{
		if (result.getName() == null)
			this.result.setName(getName());
		else if (!result.getName().equals(getName()))
		{	
			DoubleValue dv = new DoubleValueFunction(result.doubleValue(), "COPY", new DoubleValue[]{result});
			dv.setName(getName());
			return dv;
		}	
	
		return this.result;
	}
	
	public void setResult(DoubleValue result)
	{
		this.result = result;
	}

}
