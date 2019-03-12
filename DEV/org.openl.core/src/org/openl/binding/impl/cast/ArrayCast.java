package org.openl.binding.impl.cast;

import java.lang.reflect.Array;

import org.openl.types.IOpenClass;

final class ArrayCast implements IOpenCast {

    private IOpenClass toComponentType;
    private IOpenCast openCast;
    private int distance;

    ArrayCast(IOpenClass to, IOpenCast openCast) {
        this.toComponentType = to;
        this.openCast = openCast;
        this.distance = CastFactory.ARRAY_CAST_DISTANCE + openCast.getDistance();
    }

    public Object convert(Object from) {
        if (from == null) {
            return null;
        }
        Class<?> fromClass = from.getClass();
        Class<?> toClass = toComponentType.getInstanceClass();
        if (!fromClass.isArray()) {
            throw new ClassCastException(fromClass.getSimpleName() + " can't be cast to " + toClass.getCanonicalName());
        }
        int length = Array.getLength(from);
        Object convertedArray = Array.newInstance(toClass, length);
        for (int i = 0; i < length; i++) {
            Object fromValue = Array.get(from, i);
            Object toValue = openCast.convert(fromValue);
            Array.set(convertedArray, i, toValue);
        }
        return convertedArray;
    }

    public int getDistance() {
        return distance;
    }

    public boolean isImplicit() {
        return openCast.isImplicit();
    }

}
