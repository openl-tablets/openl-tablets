package org.openl.types.impl;

public enum CollectionType {
    ARRAY,
    LIST,
    MAP;

    public boolean isArray() {
        return ARRAY.equals(this);
    }

    public boolean isList() {
        return LIST.equals(this);
    }

    public boolean isMap() {
        return MAP.equals(this);
    }

}
