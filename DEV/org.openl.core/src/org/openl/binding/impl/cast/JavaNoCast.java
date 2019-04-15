package org.openl.binding.impl.cast;

final class JavaNoCast implements IOpenCast {

    private static final JavaNoCast INSTANCE = new JavaNoCast();

    private JavaNoCast() {
        // Use JavaNoCast.getInstance().
    }
    
    static JavaNoCast getInstance() {
        return INSTANCE;
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
