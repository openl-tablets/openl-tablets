package org.openl.binding.impl.cast;

import java.lang.reflect.Array;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.Nullable;

import org.openl.types.IOpenClass;
import org.openl.util.ArrayElements;

@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class ArrayCast implements IOpenCast {

    private final IOpenClass toComponentType;
    private final IOpenCast openCast;

    @Override
    public @Nullable Object convert(Object from) {
        if (from == null) {
            return null;
        }
        Class<?> fromClass = from.getClass();
        Class<?> toClass = toComponentType.getInstanceClass();
        if (!fromClass.isArray()) {
            throw new ClassCastException(
                    "Cannot cast '%s' to '%s'.".formatted(fromClass.getTypeName(), toClass.getTypeName()));
        }
        var length = Array.getLength(from);
        Object convertedArray = Array.newInstance(toClass, length);
        for (var i = 0; i < length; i++) {
            Object fromValue = ArrayElements.get(from, i);
            var toValue = openCast.convert(fromValue);
            ArrayElements.set(convertedArray, i, toValue);
        }
        return convertedArray;
    }

    @Override
    public int getDistance() {
        return openCast.getDistance();
    }

    @Override
    public boolean isImplicit() {
        return openCast.isImplicit();
    }

}
