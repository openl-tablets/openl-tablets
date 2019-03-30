package org.openl.binding.impl.cast;

final class JavaNoCast implements IOpenCast {

    public static final IOpenCast INSTANCE = new JavaNoCast();

    private JavaNoCast() {
        // Use JavaNoCast.instance.
    }

    @Override
    public Object convert(Object from) {
        return from;
    }

    @Override
    public int getDistance() {
        return CastFactory.NO_CAST_DISTANCE;
    }

    @Override
    public boolean isImplicit() {
        return true;
    }
}
