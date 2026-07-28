package org.openl.spring.env;

import java.util.Collections;
import java.util.Map;

import lombok.Getter;

public class PropertyBean {

    @Getter
    private final Map<String, String> defaultPropertyMap;
    @Getter
    private final Map<String, String> propertyMap;

    public PropertyBean(Map<String, String> defaultPropertyMap, Map<String, String> propertyMap) {
        this.defaultPropertyMap = Collections.unmodifiableMap(defaultPropertyMap);
        this.propertyMap = Collections.unmodifiableMap(propertyMap);
    }
}
