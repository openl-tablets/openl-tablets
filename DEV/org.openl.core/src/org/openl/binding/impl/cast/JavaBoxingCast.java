package org.openl.binding.impl.cast;

/**
 * Emulates type boxing.
 */
public class JavaBoxingCast implements IOpenCast {

    private final int distance;

    public JavaBoxingCast() {
        this(CastFactory.JAVA_BOXING_CAST_DISTANCE);
    }

    public JavaBoxingCast(int distance) {
        this.distance = distance;
    }

    public Object convert(Object from) {
        return from;
    }

    public int getDistance() {
        return distance;
    }

    public boolean isImplicit() {
        return true;
    }

}
