/*
 * Created on Mar 2, 2004
 *
 * Developed by OpenRules Inc. 2003-2004
 */
 
package org.openl.util;

import java.util.ArrayList;
import java.util.Comparator;




/**
 * @author snshor
 * PriorityQueue returns elements (using method pop()) in order Comparator.less(), first-in-first-out
 * 
 * This version is NOT synchronized
 *
 */
public class PriorityQueue extends ArrayList
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 5240446330214701778L;
	Comparator comparator;


	/**
	 * @param initialCapacity
	 */
	public PriorityQueue(int initialCapacity, Comparator comparator)
	{
		super(initialCapacity);
		this.comparator = comparator;
	}


	/**
	 * 
	 */
	public PriorityQueue(Comparator comparator)
	{
		super();
		this.comparator = comparator;
		
	}
	
	
	public void push(Object obj)
	{
		int len = super.size();
		int i = 0;
		for (; i < len; i++)
		{
			int cmp = comparator.compare(obj, get(i));
			
			if (cmp >= 0)
			  break;			
		}
		
		super.add(i, obj);
	}
	
	public Object pop()
	{
		return super.remove(super.size() - 1);
	}
	
	public Object top()
	{
		return super.get(super.size() - 1);
	}
	
	
	
	

}
