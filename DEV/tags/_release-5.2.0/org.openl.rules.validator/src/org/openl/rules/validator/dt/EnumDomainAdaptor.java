/**
 * Created Feb 12, 2007
 */
package org.openl.rules.validator.dt;

import org.openl.domain.EnumDomain;

import com.exigen.ie.constrainer.IntVar;

/**
 * @author snshor
 *
 */
public class EnumDomainAdaptor implements IDomainAdaptor
{
	Object[] values;

	public EnumDomainAdaptor(Object[] values)
	{
		this.values = values;
	}
	
	public EnumDomainAdaptor(EnumDomain<?> d)
	{
		this.values = d.getEnum().getAllObjects(); 
	}

	public Object[] getValues()
	{
		return this.values;
	}

	public void setValues(Object[] values)
	{
		this.values = values;
	}

	public int size()
	{
		return values.length;
	}

	public int getIndex(Object value)
	{
		for (int i = 0; i < values.length; i++)
		{
			if (values[i].equals(value))
				return i;
		}
		return -1;
	}

	public Object getValue(int index)
	{
		return values[index];
	}

	public int getMin()
	{
		return 0;
	}

	public int getMax()
	{
		return values.length-1;
	}

	public int getIntVarDomainType() {
		return IntVar.DOMAIN_BIT_FAST;
	}
	
	
	

}
