package org.openl.binding.impl.cast;

public final class ThrowableVoidCast implements IOpenCast {

    /**
     * for error("message") method
     */
    static IOpenCast instance = new ThrowableVoidCast();

    private ThrowableVoidCast() {
        // Use ThrowableVoidCast.instance.
    }

    public Object convert(Object from) {
        return null;
    }

    public int getDistance() {
        return CastFactory.THROWABLE_VOID_CAST_DISTANCE;
    }

    public boolean isImplicit() {
        return true;
    }

    public static final class ThrowableVoid {

    }

}
