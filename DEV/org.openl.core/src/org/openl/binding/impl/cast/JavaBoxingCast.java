package org.openl.binding.impl.cast;

import org.openl.types.IOpenClass;

/**
 * Emulates type boxing.
 */
public class JavaBoxingCast implements IOpenCast {

    private final int distance;

    public JavaBoxingCast() {
        this(3);
    }

    public JavaBoxingCast(int distance) {
        this.distance = distance;
    }

    public Object convert(Object from) {
        return from;
    }

    public int getDistance(IOpenClass from, IOpenClass to) {
        return distance;
    }

    public boolean isImplicit() {
        return true;
    }

}
