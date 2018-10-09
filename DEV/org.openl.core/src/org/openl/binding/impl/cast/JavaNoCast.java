package org.openl.binding.impl.cast;

final class JavaNoCast implements IOpenCast {

    public static final IOpenCast INSTANCE = new JavaNoCast();

    private JavaNoCast() {
        // Use JavaNoCast.instance.
    }

    public Object convert(Object from) {
        return from;
    }

    public int getDistance() {
        return CastFactory.NO_CAST_DISTANCE;
    }

    public boolean isImplicit() {
        return true;
    }
}
