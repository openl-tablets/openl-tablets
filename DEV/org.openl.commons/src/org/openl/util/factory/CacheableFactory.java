package org.openl.util.factory;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A cacheable factory class. It allows to create one instance of object per an
 * input parameter.
 *
 * It is thread-safe.
 *
 * @author Yury Molchan
 */
public class CacheableFactory<K, V> implements Factory<K, V> {

    private final ConcurrentMap<K, V> cache = new ConcurrentHashMap<K, V>();
    private final Factory<K, V> factory;

    public CacheableFactory(Factory<K, V> factory) {
        this.factory = factory;
    }

    /**
     * Returns the cached object instance or creates the new instance if it was
     * absent.
     * 
     * @param param the input parameter which used as a key for identifying the
     *            object instance
     * @return the cached object instance
     */
    @Override
    public V create(K param) {
        V value = cache.get(param);
        if (value == null) {
            value = factory.create(param);
            V saved = cache.putIfAbsent(param, value);
            if (saved != null) {
                // Concurrent modification happens
                // Return saved instance
                value = saved;
            }
        }
        return value;
    }
}
