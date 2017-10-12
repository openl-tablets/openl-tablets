package org.openl.rules.ruleservice.publish.lazy;

import org.ehcache.event.CacheEvent;
import org.openl.CompiledOpenClass;

public interface Event {
    void onEvent(CacheEvent<? extends Key, ? extends CompiledOpenClass> event);
}
