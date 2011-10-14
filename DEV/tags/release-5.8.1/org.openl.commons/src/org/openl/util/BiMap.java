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
    protected HashMap<K, T> idObjMap = null;
    protected IdentityHashMap<T, K> objIdMap = null;

    protected int id = 0;

    public BiMap() {
        reset();
    }

    public T get(K key) {

        return idObjMap.get(key);
    }

    public synchronized K getKey(T o) {
        return objIdMap.get(o);
    }

    final int newID() {
        return ++id;
    }

    public synchronized T put(K key, T value) {

        objIdMap.put(value, key);
        T old = idObjMap.put(key, value);
        return old;
    }

    public void removeValue(T v) {
        K key = objIdMap.get(v);

        if (key == null) {
            return;
        }

        idObjMap.remove(key);
        objIdMap.remove(v);
    }

    public void reset() {
        idObjMap = new HashMap<K, T>();
        objIdMap = new IdentityHashMap<T, K>();
    }

}
