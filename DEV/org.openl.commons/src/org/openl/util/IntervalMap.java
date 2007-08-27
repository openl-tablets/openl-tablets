/**
 * Created Jul 21, 2007
 */
package org.openl.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * @author snshor
 * 
 * 
 * IntervalMap allows for semi-open interval operations. The put operations are 
 * in terms of intervals and get operations in terms of single elements. 
 * The algorithm is based on splitting intervals on non-intersecting segments and 
 * treating them separately. 
 * Semi-open intervals are chosen to coincide with TreeMap implementation
 *
 */



/**
 * 
 *   0123456789
 *    uz      N
 *     p   p  
 *     u   u u
 *      
 *      
 */    


public class IntervalMap
{

	TreeMap map = new TreeMap();
	
	
	
	public void putInterval(Comparable fromKey,Comparable toKey, Object value)
	{
		
		
		SortedMap submap = map.subMap(fromKey, toKey);
		
		
		if (submap.size() == 0 || !submap.firstKey().equals(fromKey))
		{
			SortedMap head = map.headMap(fromKey);
			ArrayList firstList = head.size() == 0 ? new ArrayList() : 
				new ArrayList((ArrayList)head.get(head.lastKey()));
			map.put(fromKey, firstList);
			
			submap = map.subMap(fromKey, toKey);
		}	
		
		
		if (!map.containsKey(toKey))
		{
			ArrayList lastList = new ArrayList((ArrayList)submap.get(submap.lastKey()));
			map.put(toKey, lastList);
		}
		
		
		for (Iterator iter = submap.entrySet().iterator(); iter.hasNext();)
		{
			Map.Entry element = (Map.Entry) iter.next();
			
			((ArrayList)element.getValue()).add(value);
		}
	
	}
	
	
	public Object getInInterval(Comparable key)
	{
		Object res = map.get(key);
		if (res != null)
			return res;
		
		SortedMap submap =  map.headMap(key);
		
		return submap.size() == 0 ? null : submap.get(submap.lastKey());
	}



	public TreeMap treeMap()
	{
		return map;
	}
	
}
