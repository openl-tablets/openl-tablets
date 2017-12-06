package org.openl.binding.impl.cast;

import org.openl.types.IOpenClass;

public class TypeToAliasWithJavaBoxingCast extends TypeToAliasCast {

    public TypeToAliasWithJavaBoxingCast(IOpenClass from, IOpenClass to) {
        super(from, to);
    }

    @Override
    public int getDistance(IOpenClass from, IOpenClass to) {
        return CastFactory.TYPE_TO_ALIAS_WITH_JAVA_BOXING_CAST_DISTANCE;
    }

}
