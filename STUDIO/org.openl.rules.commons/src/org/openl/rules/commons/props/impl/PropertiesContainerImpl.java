package org.openl.rules.commons.props.impl;

import org.openl.rules.commons.props.*;

import java.util.Collection;
import java.util.HashMap;

public class PropertiesContainerImpl implements PropertiesContainer {
    protected HashMap<String, Property> properties;

    public PropertiesContainerImpl() {
        properties = new HashMap<String, Property>();
    }

    public boolean hasProperty(String name) {
        return properties.containsKey(name);
    }

    public Property getProperty(String name) throws PropertyException {
        Property property = properties.get(name);
        if (property == null) {
            throw new PropertyException("No such property ''{0}''", name);
        }

        return property;
    }

    public Collection<Property> getProperties() {
        return properties.values();
    }

    public void addProperty(Property property) throws PropertyTypeException {
        String name = property.getName();
        Property existing = properties.get(name);

        if (existing == null) {
            properties.put(name, property);
        } else {
            switch (property.getType()) {
                case DATE:
                    existing.setValue(property.getDate());
                    break;
                case STRING:
                    existing.setValue(property.getString());
                    break;
                default:
                    existing.setValue(property.getString());
            }
        }
    }

    public void removeProperty(String name) {
        properties.remove(name);
    }
}
