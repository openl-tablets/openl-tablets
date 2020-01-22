package org.openl.config;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.springframework.core.env.PropertyResolver;

public class ReadOnlyPropertiesHolder implements PropertiesHolder {
    protected final PropertyResolver propertyResolver;

    public ReadOnlyPropertiesHolder(PropertyResolver propertyResolver) {
        this.propertyResolver = propertyResolver;
    }

    @Override
    public PropertyResolver getPropertyResolver() {
        return propertyResolver;
    }

    @Override
    public final String getProperty(String key) {
        return doGetProperty(key);
    }

    @Override
    public final void setProperty(String key, Object value) {
        String propValue = value != null ? value.toString() : null;
        doSetProperty(key, propValue);
    }

    protected String doGetProperty(String key) {
        return propertyResolver.getProperty(key);
    }

    protected void doSetProperty(String key, String value) {
        throw new UnsupportedOperationException("Editing isn't supported");
    }

    @Override
    public void revertProperties(String... keys) {
        throw new UnsupportedOperationException("Editing isn't supported");
    }

    @Override
    public Map<String, String> getConfig() {
        throw new UnsupportedOperationException("Editing isn't supported");
    }
}
