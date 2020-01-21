package org.openl.config;

import java.util.Map;

import org.springframework.core.env.PropertyResolver;

public interface PropertiesHolder {
    void setProperty(String key, Object value);

    String getProperty(String key);

    void revertProperties(String... keys);

    Map<String, String> getConfig();

    PropertyResolver getPropertyResolver();
}
