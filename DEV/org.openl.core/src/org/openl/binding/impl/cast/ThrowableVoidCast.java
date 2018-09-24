package org.openl.binding.impl.cast;

import org.openl.types.IOpenClass;

public class ThrowableVoidCast implements IOpenCast {

    public Object convert(Object from) {
        return null;
    }

    public int getDistance(IOpenClass from, IOpenClass to) {
        return CastFactory.THROWABLE_VOID_CAST_DISTANCE;
    }

    public boolean isImplicit() {
        return true;
    }
    
    public static final class ThrowableVoid{
        
    }

}
