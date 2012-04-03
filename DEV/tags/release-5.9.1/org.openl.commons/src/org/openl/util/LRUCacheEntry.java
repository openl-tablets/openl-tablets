package org.openl.util;

/**
 * 
 * The handler interface to be used in conjunction with LastRecentlyUsedCache
 * 
 * @see LastRecentlyUsedCache
 * @author snshor
 *
 * @param <K> unique key
 * @param <T> cached object class
 */

public interface LRUCacheEntry<K, T> {
	
	/**
	 * @return cached object 
	 */
	T getHandledobject();
	
	
	/**
	 * Frees the memory,NOTE: this method should not call Sysytem.gc(). All memory management should be done on application level
	 */
	void removeHandledObject(); //cleanup
	
	
	/**
	 * 
	 * @return unique factory key identifying the object, the key must be "persistent", i.e. it should exist even if the handled object is null
	 */
	K getKey();  

}
