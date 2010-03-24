package org.openl.rules.diff.test;

import org.openl.rules.diff.hierarchy.ProjectionProperty;

public class AbstractProperty implements ProjectionProperty {
    private String name;
    private Class type;
    private Object rawValue;
    boolean comparable = true;

    public AbstractProperty(String name, Class type, Object rawValue) {
        this.name = name;
        this.type = type;
        this.rawValue = rawValue;
    }
    
    public AbstractProperty(String name, Class type, Object rawValue, boolean comparable) {
        this(name, type, rawValue);
        this.comparable = comparable;
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

    /**
     * @return the comparable
     */
    public boolean isComparable() {
        return comparable;
    }

}
