package org.openl.binding.impl.cast;

final class EnumToStringCast implements IOpenCast {
    static IOpenCast instance = new EnumToStringCast();

    private EnumToStringCast() {
        // Use EnumToStringCast.instance
    }

    @Override
    public Object convert(Object from) {
        return ((Enum<?>) from).name();
    }

    @Override
    public int getDistance() {
        return CastFactory.ENUM_TO_STRING_CAST_DISTANCE;
    }

    @Override
    public boolean isImplicit() {
        return false;
    }
}
