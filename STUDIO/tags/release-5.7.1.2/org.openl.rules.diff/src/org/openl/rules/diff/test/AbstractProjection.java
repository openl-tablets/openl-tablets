package org.openl.rules.diff.test;

import java.util.ArrayList;
import java.util.List;

import org.openl.rules.diff.hierarchy.ProjectionProperty;
import org.openl.rules.diff.hierarchy.Projection;

public class AbstractProjection implements Projection {
    private String name;
    private String type;
    private PropertySet properties;
    private List<Projection> children;

    public AbstractProjection(String name, String type) {
        this.name = name;
        this.type = type;

        properties = new PropertySet();
        children = new ArrayList<Projection>();
    }

//    @Override
    public Projection[] getChildren() {
        return children.toArray(new Projection[children.size()]);
    }

//    @Override
    public String getType() {
        return type;
    }

//    @Override
    public String getName() {
        return name;
    }

//    @Override
    public ProjectionProperty[] getProperties() {
        return properties.getAll();
    }

    public void addChild(Projection child) {
        children.add(child);
    }

    public void addProperty(ProjectionProperty property) {
        properties.add(property);
    }
}
