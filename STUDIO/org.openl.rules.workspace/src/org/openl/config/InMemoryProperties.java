package org.openl.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class InMemoryProperties implements PropertiesHolder {
    private final Map<String, Object> source;
    private final Map<String, Object> changes = new HashMap<>();

    public InMemoryProperties(Map<String, Object> source) {
        this.source = Collections.unmodifiableMap(source);
    }

    @Override
    public Map<String, Object> getProperties() {
        Map<String, Object> result = new HashMap<>(source);
        result.putAll(changes);
        return result;
    }

    @Override
    public void setProperty(String key, Object value) {
        if (key == null) {
            return;
        }

        if (value == null) {
            removeProperty(key);
            return;
        }

        changes.put(key, value.toString());
    }

    @Override
    public void removeProperty(String key) {
        changes.remove(key);
    }

    @Override
    public void setPassword(String key, String pass) {
        ConfigurationManager.setPassword(this, key, pass);
    }
}
