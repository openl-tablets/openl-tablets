package org.openl.binding.impl.cast;

import java.lang.reflect.Array;
import java.util.Objects;

import org.openl.types.IOpenClass;

final class OneElementArrayCast implements IOpenCast, IOneElementArrayCast {

    private IOpenClass toComponentType;
    private IOpenCast openCast;
    private int distance;

    OneElementArrayCast(IOpenClass to, IOpenCast openCast) {
        this.toComponentType = Objects.requireNonNull(to, "to cannot be null");
        if (this.toComponentType.isArray()) {
            throw new IllegalArgumentException("to cannot be an array type.");
        }
        this.openCast = openCast;
        this.distance = CastFactory.ONE_ELEMENT_ARRAY_CAST_DISTANCE + openCast.getDistance();
    }

    @Override
    public Object convert(Object from) {
        if (from == null) {
            return null;
        }

        Object array = Array.newInstance(toComponentType.getInstanceClass(), 1);
        Array.set(array, 0, openCast.convert(from));
        return array;
    }

    @Override
    public int getDistance() {
        return distance;
    }

    @Override
    public boolean isImplicit() {
        return openCast.isImplicit();
    }

}
