package org.openl.config;

import java.io.File;

import org.springframework.core.env.PropertyResolver;

public class ReadOnlyPropertiesHolder extends AbstractPropertiesHolder {

    public ReadOnlyPropertiesHolder(PropertyResolver propertyResolver) {
        super(propertyResolver);
    }

    @Override
    public String getProperty(String key) {
        return propertyResolver.getProperty(key);
    }

    @Override
    public String getProperty(String key, String defaultValue) {
        return propertyResolver.getProperty(key, defaultValue);
    }

    @Override
    public void setProperty(String key, Object value) {
        throw new UnsupportedOperationException("Editing isn't supported");
    }

    @Override
    public void revertProperty(String key) {
        throw new UnsupportedOperationException("Editing isn't supported");
    }

    @Override
    public void writeTo(File file) {
        throw new UnsupportedOperationException("Editing isn't supported");
    }
}
