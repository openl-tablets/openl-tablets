package org.openl.binding.impl.cast;

public class ThrowableVoidCast implements IOpenCast {

    public Object convert(Object from) {
        return null;
    }

    public int getDistance() {
        return CastFactory.THROWABLE_VOID_CAST_DISTANCE;
    }

    public boolean isImplicit() {
        return true;
    }
    
    public static final class ThrowableVoid{
        
    }

}
