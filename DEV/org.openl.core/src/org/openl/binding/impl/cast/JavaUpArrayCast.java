package org.openl.binding.impl.cast;

final class JavaUpArrayCast implements IOpenCast {

    private static final JavaUpArrayCast INSTANCE = new JavaUpArrayCast();

    private JavaUpArrayCast() {
        // Use JavaUpArrayCast.getInstance
    }

    static JavaUpArrayCast getInstance() {
        return INSTANCE;
    }

    @Override
    public Object convert(Object from) {
        return from;
    }

    @Override
    public int getDistance() {
        return CastFactory.JAVA_UP_ARRAY_TO_ARRAY_CAST_DISTANCE;
    }

    @Override
    public boolean isImplicit() {
        return true;
    }

}
