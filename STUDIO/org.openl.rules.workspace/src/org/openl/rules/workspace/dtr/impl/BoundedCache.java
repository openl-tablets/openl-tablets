package org.openl.rules.workspace.dtr.impl;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * A small synchronized least-recently-used cache.
 *
 * <p>The cache accepts non-null keys and values. Entries beyond the configured capacity are evicted.
 */
@NullMarked
final class BoundedCache<K, V> {

    private final int capacity;
    private final Map<K, V> entries = new LinkedHashMap<>(16, 0.75f, true);

    BoundedCache(int capacity) {
        if (capacity < 1) {
            throw new IllegalArgumentException("Cache capacity must be positive.");
        }
        this.capacity = capacity;
    }

    synchronized @Nullable V get(K key) {
        return entries.get(key);
    }

    synchronized void putIfAbsent(K key, V value) {
        if (entries.containsKey(key)) {
            return;
        }
        entries.put(key, value);
        if (entries.size() > capacity) {
            var iterator = entries.entrySet().iterator();
            iterator.next();
            iterator.remove();
        }
    }

    synchronized int size() {
        return entries.size();
    }
}
