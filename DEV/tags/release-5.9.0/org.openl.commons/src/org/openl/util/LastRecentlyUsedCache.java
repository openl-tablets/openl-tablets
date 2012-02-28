package org.openl.util;

import java.util.HashMap;
import org.openl.util.FastLinkedList.Entry;

/**
 * 
 * Keeps up to specific number of instances, then frees the memory using LRU algorithm. In 
 * 
 * @author snshor
 * 
 */


public class LastRecentlyUsedCache<E extends LRUCacheEntry<K,T>, K, T> {

	
	private int maxLimit;
	
	LRUStatistics statistics = new LRUStatistics();

	public LastRecentlyUsedCache()
	{
		this(300);
	}

	public LastRecentlyUsedCache(int maxLimit) {
		
		this.maxLimit = maxLimit;
	}
	
	
	FastLinkedList<E> lruList = new FastLinkedList<E>();
	HashMap<K, Entry<E>> map = new HashMap<K, FastLinkedList.Entry<E>>();
	
	
	synchronized private Entry<E> getNode(K key)
	{
		Entry<E> e = map.get(key);
		if (e != null)
		{
			// move the element at the end of the LRU list
			
			lruList.moveToLast(e);
			statistics.hit++;
			
			return e;
		}
		
		statistics.miss++;
		return null;
	}
	
	synchronized public E get(K key)
	{
		Entry<E> entry = getNode(key);
		return entry == null ? null : entry.element;
	}
	
	public synchronized void put(K key, E element)
	{
		
		
		
		if (lruList.size() >= maxLimit)
		{
			//clean the first(oldest) entry of the LRU
			
			Entry<E> e = lruList.getFirstEntry();
			lruList.removeEntry(e);
			if (e.element != null)
			{
				e.element.removeHandledObject(); //cleanup
				map.remove(e.element.getKey());
				++statistics.removed;
			}	
		}
		
		lruList.addLast(element);
		Entry<E> e = lruList.getLastEntry();
		map.put(key, e);
		
	}

	public LRUStatistics getStatistics() {
		return statistics;
	}


}
