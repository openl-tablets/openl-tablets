package org.openl.binding.impl.cast;

import lombok.Getter;

final class AliasToTypeCast implements IOpenCast, INestedCastOpenCast {
    private static final AliasToTypeCast INSTANCE = new AliasToTypeCast();

    private IOpenCast typeCast;
    @Getter
    private int distance = CastFactory.ALIAS_TO_TYPE_CAST_DISTANCE;
    @Getter
    private boolean implicit = true;

    static AliasToTypeCast getInstance() {
        return INSTANCE;
    }

    private AliasToTypeCast() {
    }

    AliasToTypeCast(IOpenCast typeCast) {
        this.typeCast = typeCast;
        this.distance = typeCast.getDistance() - 1;// This cast has higher priority
        this.implicit = typeCast.isImplicit();
    }

    @Override
    public IOpenCast getNestedOpenCast() {
        return typeCast;
    }

    @Override
    public boolean hasNestedOpenCast() {
        return typeCast != null;
    }

    @Override
    public Object convert(Object from) {
        if (typeCast != null) {
            from = typeCast.convert(from);
        }

        return from;
    }
}
