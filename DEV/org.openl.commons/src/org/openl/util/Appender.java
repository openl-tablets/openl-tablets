/*
 * Created on May 21, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */
 
package org.openl.util;

import java.util.Collection;

/**
 * @author snshor
 *
 */
public class Appender implements IAppender
{
	
	public static final IAppender NUL = new Appender();
	
	/**
	 * Stores data in array
	 * @param ary
	 * @return
	 */
	public static IAppender toArray(Object[] ary)
	{
		return new ArrayAppender(ary);
	}
	
	public static IAppender toCollection(Collection cc)
	{
		return new CollectionAppender(cc);
	}
	
	
	/**
	 * Stores data in array within selected range
	 * @param ary
	 * @param from
	 * @param to
	 * @return
	 */
	public static IAppender toArray(Object[] ary, int from, int to)
	{
		return new ArrayAppender(ary, from, to);
	}
	
	
	/**
	 * NUL implementation, will append everything 
	 */

  public boolean add(Object obj)
  {
    return true;
  }
  
  
  static class ArrayAppender implements IAppender
  {
  	Object[] ary;
  	int from;
  	int to;
  	
  	ArrayAppender(Object[] ary)
  	{
  		this(ary, 0, ary.length);
  	}

		ArrayAppender(Object[] ary, int from, int to)
		{
			this.ary = ary;
			this.from = from;
			this.to = to;
		}	
  	
    /* (non-Javadoc)
     * @see org.openl.util.IAppender#add(java.lang.Object)
     */
    public boolean add(Object obj)
    {
      if (from >= to)
        return false;
      ary[from++] = obj;
      return true;   
    }

  }

	static class CollectionAppender implements IAppender
	{
		Collection collection;
  	
		CollectionAppender(Collection collection)
		{
			this.collection = collection;
		}

  	
		/* (non-Javadoc)
		 * @see org.openl.util.IAppender#add(java.lang.Object)
		 */
		public boolean add(Object obj)
		{
			collection.add(obj);
			return true;   
		}

	}



}
