package org.openl.binding.impl.cast;

final class JavaUpCast implements IOpenCast {

    static final IOpenCast instance = new JavaUpCast();

    private JavaUpCast() {
        // Use JavaUpCast.instance.
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
