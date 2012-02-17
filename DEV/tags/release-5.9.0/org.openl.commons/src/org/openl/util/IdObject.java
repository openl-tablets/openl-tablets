package org.openl.util;

/**
 *
 */
public class IdObject {
    public int id;

    public IdObject() {
    }

    public IdObject(int id) {
        this.id = id;
    }

    final public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "id = " + id;
    }

}
