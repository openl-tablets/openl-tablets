package org.openl.rules.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;

/**
 * A set of util methods to work with arrays.
 *
 * Note: For OpenL rules only! Don't use it in Java code.
 *
 * @author Yury Molchan
 */
public final class Arrays {

    private Arrays() {
        // Utility class
    }

    /**
     * Checks if an array is empty or null.
     *
     * @param array the array to test
     * @return true if the array is empty or null
     */
    public static <T> boolean isEmpty(T[] array) {
        return array == null || array.length == 0;
    }

    /**
     * Checks if an array is NOT empty or null.
     *
     * @param array the array to test
     * @return true if the array is NOT empty or null
     */
    public static <T> boolean isNotEmpty(T[] array) {
        return !isEmpty(array);
    }

    public static <T> int length(T[] array) {
        return array == null ? 0 : array.length;
    }

    // ADD

    /**
     * Just concatenates the given elements to the array.
     */
    public static <T> T[] add(T... elements) {
        return elements;
    }

    /**
     * <p>
     * Copies the given array and adds the given elements at the end of the new array.
     * </p>
     * <p/>
     * <p>
     * The new array contains the same elements of the input array plus the given elements in the last position. The
     * component type of the new array is the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, a new one element array is returned whose component type is the same as
     * the element, unless the element itself is null, in which case the return type is Object[]
     * </p>
     * <p/>
     *
     * <pre>
     * Arrays.add(null, null)      = [null]
     * Arrays.add(null, "a")       = ["a"]
     * Arrays.add(["a"], null)     = ["a", null]
     * Arrays.add(["a"], "b")      = ["a", "b"]
     * Arrays.add(["a", "b"], "c") = ["a", "b", "c"]
     * </pre>
     *
     * @param array the array to "add" the element to, may be <code>null</code>
     * @param elements the objects to add
     * @return A new array containing the existing elements plus the new elements. The returned array type will be that
     *         of the input array (unless null), in which case it will have the same type as the element.
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] add(T[] array, T... elements) {
        if (array == null) {
            return clone(elements);
        } else if (elements == null) {
            return clone(array);
        }
        Object res = Array.newInstance(
            elements.getClass().getComponentType() == array.getClass().getComponentType()
                                                                                          ? array.getClass()
                                                                                              .getComponentType()
                                                                                          : Object.class,
            array.length + elements.length);
        System.arraycopy(array, 0, res, 0, array.length);
        for (int i = 0; i < elements.length; i++) {
            Array.set(res, array.length + i, elements[i]);
        }
        return (T[]) res;
    }

    public static <T> T[] add(T[] array, T element) {
        Object res;
        if (array != null) {
            res = Array.newInstance(
                element != null && element.getClass() == array.getClass().getComponentType()
                                                                                             ? array.getClass()
                                                                                                 .getComponentType()
                                                                                             : Object.class,
                array.length + 1);
            System.arraycopy(array, 0, res, 0, array.length);
            Array.set(res, array.length, element);
        } else {
            res = Array.newInstance(element != null ? element.getClass() : Object.class, 1);
            Array.set(res, 0, element);
        }
        return (T[]) res;
    }

    /**
     * <p>
     * Adds all the elements of the given arrays into a new array.
     * </p>
     * <p>
     * The new array contains all of the element of <code>arrays</code>. When an array is returned, it is always a new
     * array.
     * </p>
     * <p/>
     *
     * <pre>
     * Arrays.add(null, null)     = null
     * Arrays.add(array1, null)   = cloned copy of array1
     * Arrays.add(null, array2)   = cloned copy of array2
     * Arrays.add([], [])         = []
     * Arrays.add([null], [null]) = [null, null]
     * Arrays.add(["a", "b", "c"], ["1", "2", "3"]) = ["a", "b", "c", "1", "2", "3"]
     * </pre>
     *
     * @param arrays the arrays whose elements are added to the new array, may be <code>null</code>
     * @return The new array, <code>null</code> if both arrays are <code>null</code>. The type of the new array is the
     *         same type of the arrays.
     */
    @SuppressWarnings("unchecked")
    public static <T> T[] add(T[]... arrays) {
        if (arrays == null) {
            return null;
        }
        return (T[]) java.util.Arrays.stream(arrays)
            .filter(Objects::nonNull)
            .flatMap(java.util.Arrays::stream)
            .map(e -> arrays.getClass().getComponentType().getComponentType().cast(e))
            .toArray();
    }

    @Deprecated
    public static <T> T[] addAll(T[] a1, T[] a2) {
        return add(a1, a2);
    }

    /**
     * <p>
     * Inserts the specified element at the specified position in the array. Shifts the element currently at that
     * position (if any) and any subsequent elements to the right (adds one to their indices).
     * </p>
     * <p/>
     * <p>
     * This method returns a new array with the same elements of the input array plus the given element on the specified
     * position. The component type of the returned array is always the same as that of the input array.
     * </p>
     * <p/>
     * <p>
     * If the input array is <code>null</code>, a new one element array is returned whose component type is the same as
     * the element.
     * </p>
     * <p/>
     *
     * <pre>
     * Arrays.add(null, 0, null)      = [null]
     * Arrays.add(null, 0, "a")       = ["a"]
     * Arrays.add(["a"], 1, null)     = ["a", null]
     * Arrays.add(["a"], 1, "b")      = ["a", "b"]
     * Arrays.add(["a", "b"], 3, "c") = ["a", "b", "c"]
     * </pre>
     *
     * @param array the array to add the element to, may be <code>null</code>
     * @param index the position of the new object
     * @param elements the objects to add
     * @return A new array containing the existing elements and the new element
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index > array.length).
     */
    public static <T> T[] add(T[] array, int index, T... elements) {
        Class<?> type = null;
        int length = 0;
        if (elements != null) {
            type = elements.getClass().getComponentType();
            length = elements.length;
        }
        if (array != null) {
            type = array.getClass().getComponentType();
            length += array.length;
        }

        ArrayList<T> arr = new ArrayList<>(length);
        if (array != null) {
            Collections.addAll(arr, array);
        }
        if (isNotEmpty(elements)) {
            arr.addAll(index, java.util.Arrays.asList(elements));
        }
        return type == null ? null : arr.toArray((T[]) Array.newInstance(type, 0));
    }

    // SLICE
    public static <T> T[] slice(T[] values, int startIndexInclusive) {
        return slice(values, startIndexInclusive, Integer.MAX_VALUE);
    }

    public static <T> T[] slice(T[] values, int startIndexInclusive, int endIndexExclusive) {
        @SuppressWarnings("unchecked") // OK, because array is of type T
        T[] subarray = (T[]) subarray(values, startIndexInclusive, endIndexExclusive);
        return subarray;
    }

    private static Object subarray(Object array, int beginIndex, int endIndex) {
        if (array == null) {
            return null;
        }
        // handle negatives
        int size = Array.getLength(array);
        if (size == 0) {
            return array;
        }
        if (endIndex < 0) {
            endIndex = size + endIndex; // remember end is negative
        }
        if (beginIndex < 0) {
            beginIndex = size + beginIndex; // remember start is negative

        }

        // check length next
        if (endIndex > size) {
            endIndex = size;
        }

        if (beginIndex < 0) {
            beginIndex = 0;
        }
        if (endIndex < 0) {
            endIndex = 0;
        }

        final int newSize = endIndex - beginIndex;
        if (newSize == size && beginIndex == 0) {
            return array;
        }
        final Class<?> type = array.getClass().getComponentType();
        if (newSize <= 0) {
            return Array.newInstance(type, 0);
        }
        final Object subarray = Array.newInstance(type, newSize);
        System.arraycopy(array, beginIndex, subarray, 0, newSize);
        return subarray;
    }

    private static <T> T[] clone(final T[] array) {
        return array == null ? null : array.clone();
    }
}
