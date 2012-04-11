package org.openl.rules.helpers;


public class DoubleHolder implements IDoubleHolder
{
	public DoubleHolder(double value)
	{
		this.value = value;
	}

	public DoubleHolder()
	{
	}
	
	double value;

	public double getValue()
	{
		return value;
	}

	public void setValue(double value)
	{
		this.value = value;
	}
	
}