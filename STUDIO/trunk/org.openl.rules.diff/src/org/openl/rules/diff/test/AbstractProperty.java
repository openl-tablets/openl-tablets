package org.openl.rules.diff.test;

import org.openl.rules.diff.hierarchy.ProjectionProperty;

public class AbstractProperty implements ProjectionProperty {
    private String name;
    private Class type;
    private Object rawValue;

    public AbstractProperty(String name, Class type, Object rawValue) {
        this.name = name;
        this.type = type;
        this.rawValue = rawValue;
    }

//    @Override
    public String getName() {
        return name;
    }

//    @Override
    public Object getRawValue() {
        return rawValue;
    }

//    @Override
    public Class getType() {
        return type;
    }
}
