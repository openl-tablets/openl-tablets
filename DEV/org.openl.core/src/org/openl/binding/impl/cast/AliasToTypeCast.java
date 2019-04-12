package org.openl.binding.impl.cast;

final class AliasToTypeCast implements IOpenCast {
    private static final AliasToTypeCast INSTANCE = new AliasToTypeCast();

    private IOpenCast typeCast;
    private int distance = CastFactory.ALIAS_TO_TYPE_CAST_DISTANCE;

    static AliasToTypeCast getInstance() {
        return INSTANCE;
    }

    private AliasToTypeCast() {
    }

    AliasToTypeCast(IOpenCast typeCast) {
        this.typeCast = typeCast;
        distance = typeCast.getDistance() - 1;// This cast has higher priority
    }

    @Override
    public Object convert(Object from) {
        if (typeCast != null) {
            from = typeCast.convert(from);
        }

        return from;
    }

    @Override
    public int getDistance() {
        return distance;
    }

    @Override
    public boolean isImplicit() {
        return true;
    }
}
