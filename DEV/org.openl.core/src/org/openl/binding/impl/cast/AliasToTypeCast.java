package org.openl.binding.impl.cast;

import org.openl.types.IOpenClass;

public class AliasToTypeCast implements IOpenCast {

    public AliasToTypeCast(IOpenClass from, IOpenClass to) {
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
