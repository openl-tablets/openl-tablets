package org.openl.rules.diff.hierarchy;

public class AbstractProperty implements ProjectionProperty {
    private final String name;
    private final Object rawValue;

    public AbstractProperty(String name, Object rawValue) {
        this.name = name;
        this.rawValue = rawValue;
    }

    // @Override
    @Override
    public String getName() {
        return name;
    }

    // @Override
    @Override
    public Object getRawValue() {
        return rawValue;
    }
}
