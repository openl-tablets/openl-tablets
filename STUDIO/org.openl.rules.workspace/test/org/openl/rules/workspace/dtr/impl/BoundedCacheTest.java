package org.openl.rules.workspace.dtr.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class BoundedCacheTest {

    @Test
    void evictsTheLeastRecentlyUsedEntryAtCapacity() {
        var cache = new BoundedCache<String, Integer>(2);
        cache.putIfAbsent("first", 1);
        cache.putIfAbsent("second", 2);
        assertEquals(1, cache.get("first"));

        cache.putIfAbsent("third", 3);

        assertEquals(2, cache.size());
        assertEquals(1, cache.get("first"));
        assertNull(cache.get("second"));
        assertEquals(3, cache.get("third"));
    }
}
