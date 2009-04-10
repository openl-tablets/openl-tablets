/*
 * Created on Apr 28, 2004
 *
 * Developed by OpenRules Inc 2003-2004
 */
package org.openl.domain;

/**
 * @author snshor
 */
public interface IIntDomain
{
	public boolean contains(int value);
	
	public int getMin();
	public int getMax();
	
	public int size();
	
	IIntIterator intIterator();
	
}
