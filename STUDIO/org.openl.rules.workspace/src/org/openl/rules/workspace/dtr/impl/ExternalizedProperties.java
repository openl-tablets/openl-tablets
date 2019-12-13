package org.openl.rules.workspace.dtr.impl;

import java.util.Collections;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Properties that are backed with external map. It can be used to keep the order of properties in the file.
 */
class ExternalizedProperties extends Properties {
    private final Map<Object, Object> internalMap;

    ExternalizedProperties(Map<Object, Object> internalMap) {
        this.internalMap = internalMap;
    }

    @Override
    public synchronized Object put(Object key, Object value) {
        return internalMap.put(key, value);
    }

    @Override
    public synchronized Object get(Object key) {
        return internalMap.get(key);
    }

    @Override
    public synchronized Enumeration<Object> keys() {
        return Collections.enumeration(internalMap.keySet());
    }

    @Override
    public String getProperty(String key) {
        return (String) internalMap.get(key);
    }

    @Override
    public Set<Object> keySet() {
        return internalMap.keySet();
    }

    @Override
    public Set<Map.Entry<Object, Object>> entrySet() {
        return internalMap.entrySet();
    }

    @Override
    public synchronized Object remove(Object key) {
        return internalMap.remove(key);
    }
}
