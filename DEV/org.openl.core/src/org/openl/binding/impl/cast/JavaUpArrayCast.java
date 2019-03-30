package org.openl.binding.impl.cast;

final class JavaUpArrayCast implements IOpenCast {

    static IOpenCast instance = new JavaUpArrayCast();

    private JavaUpArrayCast() {
        // Use JavaUpArrayCast.instance.
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
