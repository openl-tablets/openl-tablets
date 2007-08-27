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
public interface IFiniteDomain extends IDomain
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
	 * in case when max size can not be presented as a positive integer, return REALLY_BIG 
	 *  
	 */
	
	public int maxSize(); 

	
	/**
	 * @return iterator over domain
	 */
	public Iterator iterator();
	
	
	
}
