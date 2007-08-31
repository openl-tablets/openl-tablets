/**
 * Created Aug 28, 2007
 */
package org.openl.rules.dt;

import org.openl.rules.helpers.IntRange;


/**
 * @author snshor
 *
 */
public interface IRangeAdaptor
{

	/**
	 * @author snshor
	 *
	 */
	public class IntRangeAdaptor implements IRangeAdaptor
	{

		public Comparable getMin(Object param)
		{
			return new Integer(((IntRange)param).getMin());
		}

		public Comparable getMax(Object param)
		{
			int max = ((IntRange)param).getMax();
			if (max != Integer.MAX_VALUE)
				max = max + 1;
			
				return new Integer(max);
		}

	}

	/**
	 * 
	 * @param param
	 * @return the min bound of the expression min <= X && X < max
	 */
	public Comparable getMin(Object param);
	
	/**
	 * 
	 * @param param
	 * @return the max bound of the expression min <= X && X < max
	 */
	public Comparable getMax(Object param);
	
}
