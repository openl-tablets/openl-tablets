package org.openl.binding.impl.cast;

public final class CastToAnyOpenCast implements IOpenCast {

    /**
     * for error("message") method
     */
    private static final CastToAnyOpenCast INSTANCE = new CastToAnyOpenCast();

    private CastToAnyOpenCast() {
    }

    static CastToAnyOpenCast getInstance() {
        return INSTANCE;
    }

    @Override
    public Object convert(Object from) {
        return null;
    }

    @Override
    public int getDistance() {
        return CastFactory.CAST_TO_ANY_DISTANCE;
    }

    @Override
    public boolean isImplicit() {
        return true;
    }
}
