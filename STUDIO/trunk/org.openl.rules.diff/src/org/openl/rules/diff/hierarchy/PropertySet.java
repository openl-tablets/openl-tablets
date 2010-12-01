package org.openl.rules.diff.hierarchy;

import java.util.HashMap;
import java.util.Map;


public class PropertySet {
    private Map<String, ProjectionProperty> properties;

    public PropertySet() {
        properties = new HashMap<String, ProjectionProperty>();
    }

    public ProjectionProperty get(String propertyName) {
        return properties.get(propertyName);
    }

    public void add(ProjectionProperty property) {
        String propertyName = property.getName();
        if (properties.get(propertyName) != null) {
            // property with such name exists already
            throw new IllegalArgumentException("Property '" + propertyName + "' exists already!");
        }

        properties.put(propertyName, property);
    }

    public ProjectionProperty[] getAll() {
        return properties.values().toArray(new ProjectionProperty[properties.size()]);
    }
}
