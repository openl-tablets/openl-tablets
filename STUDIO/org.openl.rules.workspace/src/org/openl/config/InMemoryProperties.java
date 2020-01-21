package org.openl.config;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.core.env.PropertyResolver;

public class InMemoryProperties extends ReadOnlyPropertiesHolder {

    private final Map<String, String> changes = new HashMap<>();
    private final Set<String> reverts = new HashSet<>();

    public InMemoryProperties(PropertyResolver propertyResolver) {
        super(propertyResolver);
    }

    @Override
    protected void doSetProperty(String key, String value) {
        if (key == null) {
            return;
        }

        if (value == null) {
            revertProperties(key);
            return;
        }

        changes.put(key, value);
    }

    @Override
    protected String doGetProperty(String key) {
        return changes.containsKey(key) ? changes.get(key) : super.doGetProperty(key);
    }

    @Override
    public void revertProperties(String... keys) {
        for (String key : keys) {
            changes.remove(key);
            reverts.add(key);
        }
    }

    @Override
    public Map<String, String> getConfig() {
        HashMap<String, String> config = new HashMap<>(changes);
        for (String revert : reverts) {
            config.put(revert, null);
        }
        return config;
    }
}
