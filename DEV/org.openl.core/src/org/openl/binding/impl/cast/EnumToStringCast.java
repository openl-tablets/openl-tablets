package org.openl.binding.impl.cast;

import org.openl.types.IOpenClass;

final class EnumToStringCast implements IOpenCast {
    public static IOpenCast instance = new EnumToStringCast();

    private EnumToStringCast() {
        // Use EnumToStringCast.instance
    }

    @Override
    public Object convert(Object from) {
        return ((Enum<?>)from).name();
    }

    @Override
    public int getDistance(IOpenClass from, IOpenClass to) {
        return CastFactory.ENUM_TO_STRING_CAST_DISTANCE;
    }

    @Override
    public boolean isImplicit() {
        return false;
    }
}
