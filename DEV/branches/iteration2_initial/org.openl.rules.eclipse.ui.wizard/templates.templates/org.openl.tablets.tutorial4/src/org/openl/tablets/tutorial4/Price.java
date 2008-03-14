/**
 * Created Jan 31, 2007
 */
package org.openl.tablets.tutorial4;

import org.openl.base.NamedThing;
import org.openl.meta.DoubleValue;
import org.openl.meta.DoubleValueFunction;

/**
 * @author snshor
 *
 */
public class Price extends NamedThing
{
	DoubleValue price = DoubleValue.ZERO;
	
	public Price(String name)
	{
		super(name);
	}

	public DoubleValue getFinalPrice()
	{
		if (price.getName() == null)
			this.price.setName(getName());
		else if (!price.getName().equals(getName()))
			return new DoubleValueFunction(price.doubleValue(), "COPY", new DoubleValue[]{price});
		return this.price;
	}

	public DoubleValue getPrice()
	{
		return this.price;
	}

	public void setPrice(DoubleValue price)
	{
		this.price = price;
	}
	
}
