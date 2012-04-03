package org.openl.binding.impl.cast;

import org.openl.types.IOpenClass;

/**
 * Emulates type boxing.
 */
public class JavaBoxingCast implements IOpenCast {

    public Object convert(Object from) {
        return from;
    }

    public int getDistance(IOpenClass from, IOpenClass to) {
        return 1;
    }

    public boolean isImplicit() {
        return true;
    }

}
