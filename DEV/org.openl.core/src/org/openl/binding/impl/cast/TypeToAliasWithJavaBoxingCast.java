package org.openl.binding.impl.cast;

import org.openl.types.IOpenClass;

final class TypeToAliasWithJavaBoxingCast extends TypeToAliasCast {

    TypeToAliasWithJavaBoxingCast(IOpenClass from, IOpenClass to) {
        super(from, to);
    }

    @Override
    public int getDistance() {
        return CastFactory.TYPE_TO_ALIAS_WITH_JAVA_BOXING_CAST_DISTANCE;
    }

}
