package org.openl.rules.diff.hierarchy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class AbstractProjection implements Projection {
    private String name;
    private String type;
    private PropertySet properties;
    private List<Projection> children;

    public AbstractProjection(String name, String type) {
        this.name = name;
        this.type = type;

        properties = new PropertySet();
        children = new ArrayList<>();
    }

    // @Override
    @Override
    public List<Projection> getChildren() {
        return Collections.unmodifiableList(children);
    }

    // @Override
    @Override
    public String getType() {
        return type;
    }

    // @Override
    @Override
    public String getName() {
        return name;
    }

    // @Override
    @Override
    public Collection<ProjectionProperty> getProperties() {
        return properties.getAll();
    }

    // @Override
    @Override
    public ProjectionProperty getProperty(String propertyName) {
        return properties.get(propertyName);
    }

    // @Override
    @Override
    public Object getPropertyValue(String propertyName) {
        ProjectionProperty p = properties.get(propertyName);
        return (p == null) ? p : p.getRawValue();
    }

    public void addChild(Projection child) {
        children.add(child);
    }

    public void addProperty(ProjectionProperty property) {
        properties.add(property);
    }
}
