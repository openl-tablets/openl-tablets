package org.openl.config;

import java.io.File;
import java.io.IOException;

import org.springframework.core.env.PropertyResolver;

public interface PropertiesHolder {
    void setProperty(String key, Object value);

    String getProperty(String key);

    void revertProperties(String... keys);

    String getPassword(String key);

    void setPassword(String key, String pass);

    void writeTo(File file) throws IOException;

    PropertyResolver getPropertyResolver();
}
