package org.openl.config;

import java.util.Map;

public interface PropertiesHolder {
    Map<String, Object> getProperties();

    void setProperty(String key, Object value);

    void removeProperty(String key);

    void setPassword(String key, String pass);
}
