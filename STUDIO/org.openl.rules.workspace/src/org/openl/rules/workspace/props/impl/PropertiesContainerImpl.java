package org.openl.rules.workspace.props.impl;

import java.util.Collection;
import java.util.HashMap;

import org.openl.rules.common.PropertiesContainer;
import org.openl.rules.common.Property;
import org.openl.rules.common.PropertyException;

/**
 * Implementation of Properties Container
 */
public class PropertiesContainerImpl implements PropertiesContainer {
    private HashMap<String, Property> properties;

    public PropertiesContainerImpl() {
        properties = new HashMap<>();
    }

    /** {@inheritDoc} */
    @Override
    public void addProperty(Property property) throws PropertyException {
        String name = property.getName();
        Property existing = properties.get(name);

        if (existing == null) {
            // add if there is no prop with such name
            properties.put(name, property);
        } else {
            // smart update
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

    /** {@inheritDoc} */
    @Override
    public Collection<Property> getProperties() {
        return properties.values();
    }

    /** {@inheritDoc} */
    @Override
    public Property getProperty(String name) throws PropertyException {
        Property property = properties.get(name);
        if (property == null) {
            throw new PropertyException("No such property ''{0}''", null, name);
        }

        return property;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasProperty(String name) {
        return properties.containsKey(name);
    }

    /**
     * For internal use.
     */
    public void removeAll() {
        properties.clear();
    }

    /** {@inheritDoc} */
    @Override
    public Property removeProperty(String name) throws PropertyException {
        // throws exception if no prop with such name
        Property prop = getProperty(name);

        properties.remove(name);
        return prop;
    }
}
