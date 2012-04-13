/**
 * 
 */
package com.exigen.le.project.cache;

import java.util.Collection;
import java.util.Properties;
import java.util.Set;

/**
 * @author vabramovs
 *
 */
public interface Cache<K,V> {
	
	
	/**
	 * @param prop
	 */
	public void init(Properties prop);
	/**
	 * @param key
	 * @return
	 */
	public V get(K key);
	/**
	 * @param key
	 * @param element
	 */
	public V put(K key,V element);
	/**
	 * @param key
	 */
	public V remove(K key);
	/**
	 * 
	 */
	public void removeAll();
	/**
	 * @return
	 */
	public Set<K> getKeys();
	/**
	 * @return
	 */
	public Collection<V> getValues();
	
}
