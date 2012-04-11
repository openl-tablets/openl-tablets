/**
 * Created Apr 6, 2007
 */
package org.openl.domain;

import java.util.Iterator;

/**
 * @author snshor
 *
 * 
 */
public interface IFiniteDomain<T> extends IDomain<T>
{
	static final public int UNKNOWN_SIZE = -1;
	static final public int REALLY_BIG = Integer.MAX_VALUE;
	
	
	/**
	 * 
	 * @return the exact size of the domain, or UNKNOWN_SIZE 
	 */
	public int size(); 
	
	/**
	 * @return the maximum size of the domain, guaranteed to be no less than actual size; 
	 * in case when max size is not known or can not be presented as a positive integer, return REALLY_BIG 
	 *  
	 */
	
	public int maxSize(); 

	/**
	 * @return the minimum size of the domain, guaranteed to be no more than actual size; 
	 * in case when min size is not known return 0 
	 *  
	 */
	
	public int minSize(); 
	
	/**
	 * @return iterator over domain
	 */
	public Iterator<T> iterator();
	
	
	public static abstract class FixedSizeDomain<T> implements IFiniteDomain<T>
	{

	    final public boolean isFinite()
	    {
		return true;
	    }

	    final public int maxSize()
	    {
		return size();
	    }

	    final public int minSize()
	    {
		return size();
	    }
	    
	}
	
	
}
