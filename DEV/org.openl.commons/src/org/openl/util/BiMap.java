/**
 *  OpenL Tablets,  2006
 *  https://sourceforge.net/projects/openl-tablets/
 */
package org.openl.util;

import java.util.HashMap;
import java.util.IdentityHashMap;

/**
 * @author snshor
 *
 */
public class BiMap<K, T> {
    private HashMap<K, T> idObjMap = new HashMap<>(1);
    private IdentityHashMap<T, K> objIdMap = new IdentityHashMap<>(1);

    public T get(K key) {
        return idObjMap.get(key);
    }

    public synchronized K getKey(T o) {
        return objIdMap.get(o);
    }

    public synchronized T put(K key, T value) {
        objIdMap.put(value, key);
        return idObjMap.put(key, value);
    }
}
