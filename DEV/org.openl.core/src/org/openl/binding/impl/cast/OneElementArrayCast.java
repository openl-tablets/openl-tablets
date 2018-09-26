package org.openl.binding.impl.cast;

import java.lang.reflect.Array;

import org.openl.types.IOpenClass;

final class OneElementArrayCast implements IOpenCast, IgnoredByMethodSearchOpenCast {

    private IOpenClass toComponentType;
    private IOpenCast openCast;
    private int distance;

    OneElementArrayCast(IOpenClass to, IOpenCast openCast) {
        if (to == null) {
            throw new IllegalArgumentException("to arg can't be null!");
        }
        if (to.isArray()) {
            throw new IllegalArgumentException("to arg can't be array type!");
        }
        this.toComponentType = to;
        this.openCast = openCast;
        this.distance = CastFactory.ONE_ELEMENT_ARRAY_CAST_DISTANCE + openCast.getDistance();
    }

    public Object convert(Object from) {
        if (from == null) {
            return null;
        }

        Object array = Array.newInstance(toComponentType.getInstanceClass(), 1);
        Array.set(array, 0, openCast.convert(from));
        return array;
    }

    public int getDistance() {
        return distance;
    }

    public boolean isImplicit() {
        return openCast.isImplicit();
    }

}
