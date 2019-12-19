package org.openl.config;

import java.io.File;

import org.springframework.core.env.PropertyResolver;

public class ReadOnlyPropertiesHolder extends AbstractPropertiesHolder {

    private final PropertyResolver propertiesResolver;

    public ReadOnlyPropertiesHolder(PropertyResolver propertiesResolver) {
        this.propertiesResolver = propertiesResolver;
    }

    @Override
    public String getProperty(String key) {
        return propertiesResolver.getProperty(key);
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
