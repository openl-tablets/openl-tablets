package org.openl.binding.impl.cast;

/**
 * Emulates type boxing.
 */
final class JavaBoxingUpCast implements IOpenCast {

    static IOpenCast instance = new JavaBoxingUpCast();

    private JavaBoxingUpCast() {
        // Use JavaBoxingUpCast.instance
    }

    @Override
    public Object convert(Object from) {
        return from;
    }

    @Override
    public int getDistance() {
        return CastFactory.JAVA_BOXING_UP_CAST_DISTANCE;
    }

    @Override
    public boolean isImplicit() {
        return true;
    }

}
