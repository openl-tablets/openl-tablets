package org.openl.binding.impl.cast;

final class JavaUpCast implements IOpenCast {

    static IOpenCast instance = new JavaUpCast();

    private JavaUpCast() {
        // Use JavaUpCast.instance.
    }

    public Object convert(Object from) {
        return from;
    }

    public int getDistance() {
        return CastFactory.JAVA_UP_CAST_DISTANCE;
    }

    public boolean isImplicit() {
        return true;
    }

}
