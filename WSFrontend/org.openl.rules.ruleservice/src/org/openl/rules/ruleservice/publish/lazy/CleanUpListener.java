package org.openl.rules.ruleservice.publish.lazy;

import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryExpiredListener;
import javax.cache.event.CacheEntryListenerException;
import javax.cache.event.CacheEntryRemovedListener;

import org.openl.CompiledOpenClass;

public class CleanUpListener implements CacheEntryRemovedListener<Key, CompiledOpenClass>, CacheEntryExpiredListener<Key, CompiledOpenClass> {

    @Override
    public void onExpired(
            Iterable<CacheEntryEvent<? extends Key, ? extends CompiledOpenClass>> cacheEntryEvents) throws CacheEntryListenerException {
        cleanUp(cacheEntryEvents);

    }

    @Override
    public void onRemoved(
            Iterable<CacheEntryEvent<? extends Key, ? extends CompiledOpenClass>> cacheEntryEvents) throws CacheEntryListenerException {
        cleanUp(cacheEntryEvents);
    }

    private void cleanUp(Iterable<CacheEntryEvent<? extends Key, ? extends CompiledOpenClass>> cacheEntryEvents) {
        cacheEntryEvents.forEach(event -> CompiledOpenClassCache.getInstance().clean(event.getKey()));
    }
}
