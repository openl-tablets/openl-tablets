package org.openl.binding.impl.cast;

/**
 * Emulates type boxing.
 */
final class JavaBoxingCast implements IOpenCast {

    private static final JavaBoxingCast INSTANCE = new JavaBoxingCast();

    private JavaBoxingCast() {
        // Use JavaBoxingCast.instance
    }

    static JavaBoxingCast getInstance() {
        return INSTANCE;
    }

    @Override
    public Object convert(Object from) {
        return from;
    }

    @Override
    public int getDistance() {
        return CastFactory.JAVA_BOXING_CAST_DISTANCE;
    }

    @Override
    public boolean isImplicit() {
        return true;
    }

}
