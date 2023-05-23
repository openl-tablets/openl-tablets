package org.openl.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.openl.spring.env.RefPropertySource;
import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertyResolver;

public class InMemoryProperties extends ReadOnlyPropertiesHolder {

    private final Map<String, String> changes = new HashMap<>();
    private final Set<String> reverts = new HashSet<>();

    public InMemoryProperties(PropertyResolver propertyResolver) {
        super(null);
        this.propertyResolver = createInMemoryPropertyResolver(propertyResolver);
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

        String oldValue = propertyResolver.getProperty(key);
        if (!value.equals(oldValue)) {
            reverts.remove(key);
            changes.put(key, value);
        }
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

    private PropertyResolver createInMemoryPropertyResolver(PropertyResolver delegate) {
        MutablePropertySources sources = new MutablePropertySources();
        sources.addLast(new MapPropertySource("inMemoryMap", Collections.unmodifiableMap(changes)));

        if (!(delegate instanceof ConfigurableEnvironment)) {
            throw new IllegalArgumentException();
        }
        MutablePropertySources delegateSources = ((ConfigurableEnvironment) delegate).getPropertySources();
        delegateSources.forEach(propertySource -> {
            if (propertySource instanceof RefPropertySource) {
                sources.addLast(new RefPropertySource(delegate, sources));
            } else {
                sources.addLast(propertySource);
            }
        });

        return new AbstractEnvironment(sources) {
        };
    }
}
