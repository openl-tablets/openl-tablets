/*
 * Created on Apr 28, 2004
 *
 * Developed by OpenRules Inc 2003-2004
 */
package org.openl.domain;


/**
 * @author snshor
 */
public class IntRange implements IIntDomain
{
	int min, max;
	

	public IntRange(int min, int max)
	{
		this.min = min;
		this.max = max;
	}

	/**
	 * @return
	 */
	public int getMax()
	{
		return max;
	}

	/**
	 * @return
	 */
	public int getMin()
	{
		return min;
	}

	public boolean contains(int value)
	{
		return min <= value && value <= max;
	}
	
	
	

	/**
	 *
	 */

	public IIntIterator iterator()
	{
		return new RangeIterator();
	}
	
	
	class RangeIterator extends AIntIterator
	{
		int current;
		
		RangeIterator()
		{
			current = min - 1;
		}
		
		/**
		 *
		 */

		public boolean hasNext()
		{
			return current < max;
		}

		/**
		 *
		 */

		public int nextInt()
		{
			return ++current;
		}

		public Object next()
		{
			return new Integer(++current);
		}

		public int size()
		{
			return max - min + 1;
		}

}

	/**
	 *
	 */

	public int size()
	{
		return max - min + 1;
	}

}
