package org.openl.rules.diff.hierarchy;

public class AbstractProperty implements ProjectionProperty {
    private String name;
    private Object rawValue;

    public AbstractProperty(String name, Object rawValue) {
        this.name = name;
        this.rawValue = rawValue;
    }

    // @Override
    public String getName() {
        return name;
    }

    // @Override
    public Object getRawValue() {
        return rawValue;
    }
}
