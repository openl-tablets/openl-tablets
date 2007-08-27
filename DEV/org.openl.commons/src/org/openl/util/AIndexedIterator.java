/*
 * Created on May 19, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.util;

import java.lang.reflect.Array;
import java.util.NoSuchElementException;

/**
 * @author snshor
 */

public abstract class AIndexedIterator extends AOpenIterator
{
	
	
	int from = 0;
	int step = 1;
	int to = -1;
			

	protected AIndexedIterator(int from, int to, int step)
	{
		this.from = from;
		this.to = to;
		this.step = step;
	}


	protected void reverseIndexes()
	{
		int s = size();
		if (s <= 0)
		  return;
		
		int newFrom = from + (s - 1) * step;
		
		step = -step;
		to = newFrom + s * step;
		from = newFrom;
	}

	
	
	public final int size()
	{
		return step > 0 ?  (to - from) / step : (from - to) /(-step) ;
	}
	
	public final boolean hasNext()
	{
		return size() > 0;
	}
	
	
	public Object next()
	{
		if (size() <= 0)
		{
			throw new NoSuchElementException();
		}
		
		int idx = from;
		from += step;
		
		return indexedElement(idx);
	}
	
	protected abstract Object indexedElement(int i);
	
	
	static class ArrayIterator extends AIndexedIterator
	{
		Object[] ary;
		
		ArrayIterator(Object[] ary)
		{
			super(0, ary.length, 1);
			this.ary = ary;
		}
		
		ArrayIterator(Object[] ary, int from, int to, int step)
		{
			super(from, to, step);
			this.ary = ary;
		}
		
		
    /* (non-Javadoc)
     * @see org.openl.util.AIndexedIterator#indexedElement(int)
     */
    protected Object indexedElement(int i)
    {
      return ary[i];
    }
    
    
    public IOpenIterator reverse()
    {
    	ArrayIterator it = new ArrayIterator(ary, from, to, step);
    	it.reverseIndexes();
    	return it;
    }

	}


	static class AnyArrayIterator extends AIndexedIterator
	{
		Object ary;
		
		AnyArrayIterator(Object ary)
		{
			super(0, Array.getLength(ary), 1);
			this.ary = ary;
		}
		
		AnyArrayIterator(Object ary, int from, int to, int step)
		{
			super(from, to, step);
			this.ary = ary;
		}
		
		
		/* (non-Javadoc)
		 * @see org.openl.util.AIndexedIterator#indexedElement(int)
		 */
		protected Object indexedElement(int i)
		{
			return Array.get(ary, i);
		}
    
    
		public IOpenIterator reverse()
		{
			AnyArrayIterator it = new AnyArrayIterator(ary, from, to, step);
			it.reverseIndexes();
			return it;
		}

	}


}
