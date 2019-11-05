package org.openl.binding.impl.cast;

import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;

import java.util.HashMap;
import java.util.Map;

final class JavaUnboxingCast implements IOpenCast {

    private static final Map<Class<?>, JavaUnboxingCast> UNBOXING_FACTORY;

    static {
        UNBOXING_FACTORY =  new HashMap<>();
        UNBOXING_FACTORY.put(Void.class, new JavaUnboxingCast(JavaOpenClass.getOpenClass(void.class)));
        UNBOXING_FACTORY.put(Boolean.class, new JavaUnboxingCast(JavaOpenClass.getOpenClass(boolean.class)));
        UNBOXING_FACTORY.put(Byte.class, new JavaUnboxingCast(JavaOpenClass.getOpenClass(byte.class)));
        UNBOXING_FACTORY.put(Short.class, new JavaUnboxingCast(JavaOpenClass.getOpenClass(short.class)));
        UNBOXING_FACTORY.put(Character.class, new JavaUnboxingCast(JavaOpenClass.getOpenClass(char.class)));
        UNBOXING_FACTORY.put(Integer.class, new JavaUnboxingCast(JavaOpenClass.getOpenClass(int.class)));
        UNBOXING_FACTORY.put(Long.class, new JavaUnboxingCast(JavaOpenClass.getOpenClass(long.class)));
        UNBOXING_FACTORY.put(Float.class, new JavaUnboxingCast(JavaOpenClass.getOpenClass(float.class)));
        UNBOXING_FACTORY.put(Double.class, new JavaUnboxingCast(JavaOpenClass.getOpenClass(double.class)));
    }

    private final IOpenClass primitiveOpenType;

    private JavaUnboxingCast(IOpenClass primitiveOpenType) {
        // Use JavaUnboxingCast#getInstance
        this.primitiveOpenType = primitiveOpenType;
    }

    static JavaUnboxingCast getInstance(Class<?> fromClass) {
        return UNBOXING_FACTORY.get(fromClass);
    }

    @Override
    public Object convert(Object from) {
        return from == null ? primitiveOpenType.nullObject() : from;
    }

    @Override
    public int getDistance() {
        return CastFactory.JAVA_UNBOXING_CAST_DISTANCE;
    }

    @Override
    public boolean isImplicit() {
        return true;
    }

}
