package org.openl.binding.impl.cast;

import java.lang.reflect.Array;
import java.util.Objects;

import org.openl.types.IOpenClass;

final class ArrayOneElementCast implements IArrayOneElementCast, IOpenCast {

    private final IOpenClass to;
    private final IOpenCast openCast;
    private final int distance;

    ArrayOneElementCast(IOpenClass to, IOpenCast openCast) {
        this.to = Objects.requireNonNull(to, "to cannot be null");
        this.openCast = openCast;
        this.distance = CastFactory.ARRAY_ONE_ELEMENT_CAST_DISTANCE + openCast.getDistance();
    }

    @Override
    public Object convert(Object from) {
        if (from == null) {
            return null;
        }

        if (from.getClass().isArray()) {
            if (Array.getLength(from) == 1) {
                return openCast.convert(Array.get(from, 0));
            } else {
                throw new ClassCastException(String.format(
                    "Cannot convert '%s' to '%s'. The number of elements in the array is '%s' instead of only one element that is expected.",
                    from.getClass().getTypeName(),
                    to.getName(),
                    Array.getLength(from)));
            }
        }
        throw new ClassCastException(
            String.format("Cannot convert from '%s' to '%s'.", from.getClass().getTypeName(), to.getName()));
    }

    @Override
    public int getDistance() {
        return distance;
    }

    @Override
    public boolean isImplicit() {
        return false;
    }
}
