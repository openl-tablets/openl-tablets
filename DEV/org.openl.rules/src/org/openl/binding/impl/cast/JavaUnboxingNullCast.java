package org.openl.binding.impl.cast;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;

public class JavaUnboxingNullCast implements IOpenCast {

    private static final Map<Class<?>, JavaUnboxingNullCast> FACTORY;

    static {
        Map<Class<?>, JavaUnboxingNullCast> factory = new HashMap<>();
        factory.put(void.class, new JavaUnboxingNullCast(JavaOpenClass.getOpenClass(void.class)));
        factory.put(boolean.class, new JavaUnboxingNullCast(JavaOpenClass.getOpenClass(boolean.class)));
        factory.put(byte.class, new JavaUnboxingNullCast(JavaOpenClass.getOpenClass(byte.class)));
        factory.put(short.class, new JavaUnboxingNullCast(JavaOpenClass.getOpenClass(short.class)));
        factory.put(char.class, new JavaUnboxingNullCast(JavaOpenClass.getOpenClass(char.class)));
        factory.put(int.class, new JavaUnboxingNullCast(JavaOpenClass.getOpenClass(int.class)));
        factory.put(long.class, new JavaUnboxingNullCast(JavaOpenClass.getOpenClass(long.class)));
        factory.put(float.class, new JavaUnboxingNullCast(JavaOpenClass.getOpenClass(float.class)));
        factory.put(double.class, new JavaUnboxingNullCast(JavaOpenClass.getOpenClass(double.class)));
        FACTORY = Collections.unmodifiableMap(factory);
    }

    private final IOpenClass primitiveOpenType;

    private JavaUnboxingNullCast(IOpenClass primitiveOpenType) {
        this.primitiveOpenType = primitiveOpenType;
    }

    static JavaUnboxingNullCast getInstance(Class<?> toClass) {
        return FACTORY.get(toClass);
    }

    @Override
    public Object convert(Object from) {
        return primitiveOpenType.nullObject();
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
