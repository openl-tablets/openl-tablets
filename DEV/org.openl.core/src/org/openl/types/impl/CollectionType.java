package org.openl.types.impl;

public enum CollectionType {
    ARRAY,
    LIST,
    MAP;

    public boolean isArray() {
        return ARRAY == this;
    }

    public boolean isList() {
        return LIST == this;
    }

    public boolean isMap() {
        return MAP == this;
    }

}
