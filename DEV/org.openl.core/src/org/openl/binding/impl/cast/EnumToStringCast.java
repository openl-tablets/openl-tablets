package org.openl.binding.impl.cast;

final class EnumToStringCast implements IOpenCast {
    private static final EnumToStringCast INSTANCE = new EnumToStringCast();

    private EnumToStringCast() {
        // Use EnumToStringCast.getInstance
    }
    
    static EnumToStringCast getInstance() {
        return INSTANCE;
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
