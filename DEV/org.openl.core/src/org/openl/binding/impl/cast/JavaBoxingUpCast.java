package org.openl.binding.impl.cast;

/**
 * Emulates type boxing.
 */
final class JavaBoxingUpCast implements IOpenCast {

    static IOpenCast instance = new JavaBoxingUpCast();

    private JavaBoxingUpCast() {
        // Use JavaBoxingUpCast.instance
    }

    public Object convert(Object from) {
        return from;
    }

    public int getDistance() {
        return CastFactory.JAVA_BOXING_UP_CAST_DISTANCE;
    }

    public boolean isImplicit() {
        return true;
    }

}
