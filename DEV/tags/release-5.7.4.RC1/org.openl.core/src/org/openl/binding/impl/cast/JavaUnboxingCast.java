package org.openl.binding.impl.cast;

import org.openl.types.IOpenClass;

public class JavaUnboxingCast implements IOpenCast {

    public Object convert(Object from) {
        return from;
    }

    public int getDistance(IOpenClass from, IOpenClass to) {
        return 2;
    }

    public boolean isImplicit() {
        return true;
    }

}
