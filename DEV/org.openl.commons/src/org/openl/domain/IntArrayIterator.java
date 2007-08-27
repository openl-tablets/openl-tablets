/**
 * Created Jul 13, 2007
 */
package org.openl.domain;


/**
 * @author snshor
 *
 */
public class IntArrayIterator extends AIntIterator
{
		int current=0;
		int[] ary;
		
		public IntArrayIterator(int[] ary)
		{
			this.ary = ary;
		}
		
		public boolean hasNext()
		{
			return current < ary.length;
		}

		/**
		 *
		 */

		public int nextInt()
		{
			return ary[current++];
		}

		public Object next()
		{
			return new Integer(ary[current++]);
		}


	public int size()
	{
		return ary.length;
	}

}
