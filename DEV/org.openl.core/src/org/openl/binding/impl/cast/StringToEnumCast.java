package org.openl.binding.impl.cast;

import org.openl.types.IOpenClass;

final class StringToEnumCast implements IOpenCast {
    @SuppressWarnings("rawtypes")
    private Class enumType;

    StringToEnumCast(Class<?> enumType) {
        this.enumType = enumType;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object convert(Object from) {
        return Enum.valueOf(enumType, (String) from);
    }

    @Override
    public int getDistance(IOpenClass from, IOpenClass to) {
        return CastFactory.STRING_ENUM_TO_CAST_DISTANCE;
    }

    @Override
    public boolean isImplicit() {
        return false;
    }
}
