package org.openl.binding.impl.cast;

final class JavaUnboxingCast implements IOpenCast {

    static IOpenCast instance = new JavaUnboxingCast();

    private JavaUnboxingCast() {
        // Use JavaUnboxingCast.instance
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
