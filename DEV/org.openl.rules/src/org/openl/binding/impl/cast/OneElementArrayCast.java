package org.openl.binding.impl.cast;

import java.lang.reflect.Array;
import java.util.Objects;

import lombok.Getter;
import org.jspecify.annotations.Nullable;

import org.openl.types.IOpenClass;
import org.openl.util.ArrayElements;

final class OneElementArrayCast implements IOpenCast, IOneElementArrayCast {

    private final IOpenClass toComponentType;
    private final IOpenCast openCast;
    @Getter
    private final int distance;

    OneElementArrayCast(IOpenClass toComponentType, IOpenCast openCast) {
        this.toComponentType = Objects.requireNonNull(toComponentType, "toComponentType cannot be null");
        this.openCast = openCast;
        this.distance = CastFactory.ONE_ELEMENT_ARRAY_CAST_DISTANCE + openCast.getDistance();
    }

    @Override
    public @Nullable Object convert(Object from) {
        if (from == null) {
            return null;
        }

        Object array = Array.newInstance(toComponentType.getInstanceClass(), 1);
        ArrayElements.set(array, 0, openCast.convert(from));
        return array;
    }

    @Override
    public boolean isImplicit() {
        return openCast.isImplicit();
    }

}
