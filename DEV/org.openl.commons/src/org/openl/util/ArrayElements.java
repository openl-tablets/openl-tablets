package org.openl.util;

import java.lang.reflect.Array;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Reads and writes the elements of an array of any type.
 * <p>
 * Behaves as {@link Array#get(Object, int)} and {@link Array#set(Object, int, Object)}: a primitive element is boxed on
 * read and unboxed on write, a primitive value is widened to the element type, and the same exceptions are thrown.
 * <p>
 * A value of the exact element type is read and written much faster than through {@link Array}, whose native calls the
 * JIT compiler cannot optimize.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@NullMarked
public final class ArrayElements {

    /**
     * Returns the element of the array at the index.
     *
     * @throws NullPointerException if the array is {@code null}
     * @throws IllegalArgumentException if the object is not an array
     * @throws ArrayIndexOutOfBoundsException if the index is outside the array
     */
    public static @Nullable Object get(Object array, int index) {
        return switch (array) {
            case Object[] a -> a[index];
            case double[] a -> a[index];
            case int[] a -> a[index];
            case long[] a -> a[index];
            case boolean[] a -> a[index];
            case float[] a -> a[index];
            case short[] a -> a[index];
            case byte[] a -> a[index];
            case char[] a -> a[index];
            default -> Array.get(array, index);
        };
    }

    /**
     * Sets the element of the array at the index.
     *
     * @throws NullPointerException if the array is {@code null}
     * @throws IllegalArgumentException if the object is not an array, or the value does not fit the element type
     * @throws ArrayIndexOutOfBoundsException if the index is outside the array
     */
    public static void set(Object array, int index, @Nullable Object value) {
        switch (array) {
            case Object[] a when value == null || a.getClass().getComponentType().isInstance(value) -> a[index] = value;
            case double[] a when value instanceof Double v -> a[index] = v;
            case int[] a when value instanceof Integer v -> a[index] = v;
            case long[] a when value instanceof Long v -> a[index] = v;
            case boolean[] a when value instanceof Boolean v -> a[index] = v;
            case float[] a when value instanceof Float v -> a[index] = v;
            case short[] a when value instanceof Short v -> a[index] = v;
            case byte[] a when value instanceof Byte v -> a[index] = v;
            case char[] a when value instanceof Character v -> a[index] = v;
            default -> Array.set(array, index, value);
        }
    }
}
