package org.openl.binding.impl.cast;

import org.openl.types.IOpenClass;

public class ThrowableVoidCast implements IOpenCast {

    public Object convert(Object from) {
        return null;
    }

    public int getDistance(IOpenClass from, IOpenClass to) {
        if (from.getInstanceClass() == null) {
            return 0;
        }

        return 6;
    }

    public boolean isImplicit() {
        return true;
    }
    
    public static final class ThrowableVoid{
        
    }

}
