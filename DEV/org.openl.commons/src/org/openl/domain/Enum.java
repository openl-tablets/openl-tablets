/*
 * Created on Apr 30, 2004
 *
 * Developed by OpenRules Inc 2003-2004
 */
package org.openl.domain;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

/**
 * @author snshor
 */
public class Enum
{

	Object[] objs;

	HashMap indexMap;

	public Enum(Object[] objs)
	{
		this.objs = objs;
		indexMap = new HashMap(objs.length);

		for (int i = 0; i < objs.length; i++)
		{
			indexMap.put(objs[i], new Integer(i));
		}
	}

	
	public boolean contains(Object obj)
	{
		return indexMap.containsKey(obj);
	}
	
	public Enum(Collection objc)
	{
		int size = objc.size();

		this.objs = new Object[size];
		this.indexMap = new HashMap(size);

		int i = 0;

		for (Iterator iter = objc.iterator(); iter.hasNext(); ++i)
		{
			Object element = (Object) iter.next();
			indexMap.put(element, new Integer(i));
		}
	}

	public int getIndex(Object obj)
	{
		Integer idx = (Integer) indexMap.get(obj);
		if (idx == null)
			throw new RuntimeException("Object " + obj + " is outside of the domain");
		return idx.intValue();
	}

	/**
	 * 
	 */
	public int size()
	{
		return objs.length;
	}

}
