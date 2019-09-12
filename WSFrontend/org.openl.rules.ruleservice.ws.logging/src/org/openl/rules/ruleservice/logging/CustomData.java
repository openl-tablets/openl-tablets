package org.openl.rules.ruleservice.logging;

import java.util.HashMap;
import java.util.Map;

public class CustomData {

    private Map<String, Object> values = new HashMap<>();

    public Object getValue(String key) {
        return values.get(key);
    }

    public void setValue(String key, Object value) {
        this.values.put(key, value);
    }

}
