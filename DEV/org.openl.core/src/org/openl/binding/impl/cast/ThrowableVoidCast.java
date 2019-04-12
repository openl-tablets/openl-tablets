package org.openl.binding.impl.cast;

public final class ThrowableVoidCast implements IOpenCast {

    /**
     * for error("message") method
     */
    private static final ThrowableVoidCast INSTANCE = new ThrowableVoidCast();

    private ThrowableVoidCast() {
        // Use ThrowableVoidCast.getInstance
    }

    static ThrowableVoidCast getInstance() {
        return INSTANCE;
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
