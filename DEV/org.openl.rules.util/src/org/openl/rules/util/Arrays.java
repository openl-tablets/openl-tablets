package org.openl.rules.util;

import java.lang.reflect.Array;

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
}
