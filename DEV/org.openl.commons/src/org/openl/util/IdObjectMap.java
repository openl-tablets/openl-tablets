/**
 *  OpenL Tablets,  2006
 *  https://sourceforge.net/projects/openl-tablets/ 
 */
package org.openl.util;

import java.util.HashMap;

/**
 * @author snshor
 *
 */
public class IdObjectMap
{
	HashMap idObjMap = null;
	HashMap objIdMap = null;
	
	int id = 0;
	
	final int newID()
	{
		return ++id;
	}
	
	public IdObjectMap()
	{
		reset();
	}
	
	
	public void reset()
	{
		idObjMap = new HashMap();
		objIdMap = new HashMap();
	}
	

	public synchronized int addObject(Object o)
	{
		Integer v = (Integer)objIdMap.get(o);  
		
		if (v == null)
		{
			v = new Integer(newID());
			objIdMap.put(o, v);
			idObjMap.put(v, o);
		}	
			
		
		return v.intValue(); 
	}

	
	public synchronized int getID(Object o)
	{
		return addObject(o); 
	}

	
	/**
	 * Always produces newID, use with caution
	 * @param o
	 * @return
	 */
	public synchronized int getNewID(Object o)
	{
		Integer v = new Integer(newID());
		objIdMap.put(o, v);
		idObjMap.put(v, o);
	
		return v.intValue(); 
	}
	

	
	public Object getObject(int id)
	{
		Integer v = new Integer(id);
		
		return idObjMap.get(v);
	}
	
	public synchronized int removeObject(Object o)
	{
		Integer v = (Integer)objIdMap.get(o);  
		
		if (v != null)
		{
			objIdMap.remove(o);
			idObjMap.remove(v);
			return v.intValue();
		}	
			
		
		return -1; 
	}
	

	
	
	public synchronized int remove(int id)
	{
		
		
		Integer v = new Integer(id);  
		
		Object obj = idObjMap.get(v);
		
		
		if (obj != null)
		{
			objIdMap.remove(obj);
			idObjMap.remove(v);
			return v.intValue();
		}	
			
		
		return -1; 
	}
	
	
	

}
