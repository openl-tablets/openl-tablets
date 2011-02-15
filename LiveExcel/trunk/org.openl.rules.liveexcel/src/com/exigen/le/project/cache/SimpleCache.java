/**
 * 
 */
package com.exigen.le.project.cache;

import java.util.Collection;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple Cache on ConcurrentHashMap base
 * @author vabramovs
 *
 */
public class SimpleCache<K,V> extends ConcurrentHashMap<K, V> implements Cache<K,V> {
	static final long   serialVersionUID =1;
	public SimpleCache(){
		super();
	}


	/* (non-Javadoc)
	 * @see com.exigen.le.project.cache.Cache#getKeys()
	 */
	
	public Set<K> getKeys() {
		return super.keySet();
	}

	/* (non-Javadoc)
	 * @see com.exigen.le.project.cache.Cache#getValues()
	 */
	
	public Collection<V> getValues() {
		return super.values();
	}

	/* (non-Javadoc)
	 * @see com.exigen.le.project.cache.Cache#removeAll()
	 */
	
	public void removeAll() {
		super.clear();
	}

	
	public void init(Properties prop) {
		
	}

}
