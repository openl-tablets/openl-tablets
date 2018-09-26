package org.openl.binding.impl.cast;

final class AliasToTypeCast implements IOpenCast {

    static IOpenCast instance = new AliasToTypeCast();

    private AliasToTypeCast() {
        // Use AliasToTypeCast.instance.
    }

    public Object convert(Object from) {
        return from;
    }

    public int getDistance() {
        return CastFactory.ALIAS_TO_TYPE_CAST_DISTANCE;
    }

    public boolean isImplicit() {
        return true;
    }
}
