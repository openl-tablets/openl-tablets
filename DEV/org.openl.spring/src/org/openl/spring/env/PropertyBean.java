package org.openl.spring.env;

import java.util.Collections;
import java.util.Map;

public class PropertyBean {

    private final Map<String, String> defaultPropertyMap;
    private final Map<String, String> propertyMap;

    public PropertyBean(Map<String, String> defaultPropertyMap, Map<String, String> propertyMap) {
        this.defaultPropertyMap = Collections.unmodifiableMap(defaultPropertyMap);
        this.propertyMap = Collections.unmodifiableMap(propertyMap);
    }

    public Map<String, String> getDefaultPropertyMap() {
        return defaultPropertyMap;
    }

    public Map<String, String> getPropertyMap() {
        return propertyMap;
    }
}
