package org.openl.binding.impl.cast;

final class JavaUnboxingCast implements IOpenCast {

    static IOpenCast instance = new JavaUnboxingCast();

    private JavaUnboxingCast() {
        // Use JavaUnboxingCast.instance
    }

    public Object convert(Object from) {
        return from;
    }

    public int getDistance() {
        return CastFactory.JAVA_UNBOXING_CAST_DISTANCE;
    }

    public boolean isImplicit() {
        return true;
    }

}
