package org.openl.rules.commons.props;

import java.util.Collection;

public interface PropertiesContainer {
    boolean hasProperty(String name);
    Property getProperty(String name) throws PropertyException;
    Collection<Property> getProperties();

    void addProperty(Property property) throws PropertyTypeException;
    void removeProperty(String name);
}
