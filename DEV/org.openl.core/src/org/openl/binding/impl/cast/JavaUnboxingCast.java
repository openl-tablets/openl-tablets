package org.openl.binding.impl.cast;

final class JavaUnboxingCast implements IOpenCast {

    private static final JavaUnboxingCast INSTANCE = new JavaUnboxingCast();

    private JavaUnboxingCast() {
        // Use JavaUnboxingCast.getInstance
    }

    static JavaUnboxingCast getInstance() {
        return INSTANCE;
    }

    @Override
    public Object convert(Object from) {
        return from;
    }

    @Override
    public int getDistance() {
        return CastFactory.JAVA_UNBOXING_CAST_DISTANCE;
    }

    @Override
    public boolean isImplicit() {
        return true;
    }

}
