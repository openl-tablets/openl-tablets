package org.openl.binding.impl.cast;

public final class ThrowableVoidCast implements IOpenCast {

    /**
     * for error("message") method
     */
    static IOpenCast instance = new ThrowableVoidCast();

    private ThrowableVoidCast() {
        // Use ThrowableVoidCast.instance.
    }

    @Override
    public Object convert(Object from) {
        return null;
    }

    @Override
    public int getDistance() {
        return CastFactory.THROWABLE_VOID_CAST_DISTANCE;
    }

    @Override
    public boolean isImplicit() {
        return true;
    }

    public static final class ThrowableVoid {

    }

}
