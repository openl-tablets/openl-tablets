package org.openl.binding.impl.cast;

import org.openl.types.IOpenClass;

public class TypeToAliasWithJavaUnboxingCast extends TypeToAliasCast {

    public TypeToAliasWithJavaUnboxingCast(IOpenClass from, IOpenClass to) {
        super(from, to);
    }

    @Override
    public int getDistance(IOpenClass from, IOpenClass to) {
        return CastFactory.TYPE_TO_ALIAS_WITH_JAVA_UNBOXING_CAST_DISTANCE;
    }

}
