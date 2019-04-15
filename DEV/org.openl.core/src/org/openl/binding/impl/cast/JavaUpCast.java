package org.openl.binding.impl.cast;

final class JavaUpCast implements IOpenCast {

    private static final JavaUpCast INSTANCE = new JavaUpCast();

    private JavaUpCast() {
        // Use JavaUpCast.getInstance
    }

    static JavaUpCast getInstance() {
        return INSTANCE;
    }

    @Override
    public Object convert(Object from) {
        return from;
    }

    @Override
    public int getDistance() {
        return CastFactory.JAVA_UP_CAST_DISTANCE;
    }

    @Override
    public boolean isImplicit() {
        return true;
    }

}
