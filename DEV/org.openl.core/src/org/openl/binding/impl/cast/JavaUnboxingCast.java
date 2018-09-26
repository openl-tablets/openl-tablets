package org.openl.binding.impl.cast;

public class JavaUnboxingCast implements IOpenCast {

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
