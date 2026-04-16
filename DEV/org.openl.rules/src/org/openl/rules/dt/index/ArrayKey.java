package org.openl.rules.dt.index;

import java.util.Arrays;

/**
 * A wrapper for array values used as keys in index maps.
 * Provides proper {@code hashCode()} and {@code equals()} based on array content,
 * since raw Java arrays use identity comparison which breaks HashMap lookups.
 */
final class ArrayKey {

    private final Object[] array;
    private final int hashCode;

    ArrayKey(Object[] array) {
        this.array = array;
        this.hashCode = Arrays.deepHashCode(array);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ArrayKey other)) {
            return false;
        }
        return Arrays.deepEquals(array, other.array);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return Arrays.deepToString(array);
    }

    /**
     * Wraps the value in an ArrayKey if it is an array, otherwise returns the value as-is.
     */
    static Object wrapIfArray(Object value) {
        if (value instanceof Object[] arr) {
            return new ArrayKey(arr);
        }
        return value;
    }
}
