package org.openl.binding.impl.cast;

import java.lang.reflect.Array;

import org.openl.types.IOpenClass;

public class OneElementArrayCast implements IOpenCast, IgnoredByMethodSearchOpenCast {

    private IOpenClass toComponentType;
    private IOpenCast openCast;

    public OneElementArrayCast(IOpenClass to, IOpenCast openCast) {
        if (to == null) {
            throw new IllegalArgumentException("to arg can't be null!");
        }
        if (to.isArray()) {
            throw new IllegalArgumentException("to arg can't be array type!");
        }
        this.toComponentType = to;
        this.openCast = openCast;
    }

    public Object convert(Object from) {
        if (from == null) {
            return null;
        }

        Object array = Array.newInstance(toComponentType.getInstanceClass(), 1);
        Array.set(array, 0, openCast.convert(from));
        return array;
    }

    public int getDistance(IOpenClass from, IOpenClass to) {
        return CastFactory.ONE_ELEMENT_ARRAY_CAST_DISTANCE + openCast.getDistance(from, to);
    }

    public boolean isImplicit() {
        return openCast.isImplicit();
    }

}

