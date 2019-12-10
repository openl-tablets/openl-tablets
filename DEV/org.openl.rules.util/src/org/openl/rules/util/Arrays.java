package org.openl.rules.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

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

    public static boolean isEmpty(Collection<?> array) {
        return array == null || array.isEmpty();
    }

    public static boolean isEmpty(byte[] array) {
        return array == null || array.length == 0;
    }

    public static boolean isEmpty(char[] array) {
        return array == null || array.length == 0;
    }

    public static boolean isEmpty(short[] array) {
        return array == null || array.length == 0;
    }

    public static boolean isEmpty(int[] array) {
        return array == null || array.length == 0;
    }

    public static boolean isEmpty(long[] array) {
        return array == null || array.length == 0;
    }

    public static boolean isEmpty(float[] array) {
        return array == null || array.length == 0;
    }

    public static boolean isEmpty(double[] array) {
        return array == null || array.length == 0;
    }

    public static boolean isEmpty(boolean[] array) {
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

    public static boolean isNotEmpty(Collection<?> array) {
        return !isEmpty(array);
    }

    public static boolean isNotEmpty(byte[] array) {
        return !isEmpty(array);
    }

    public static boolean isNotEmpty(char[] array) {
        return !isEmpty(array);
    }

    public static boolean isNotEmpty(short[] array) {
        return !isEmpty(array);
    }

    public static boolean isNotEmpty(int[] array) {
        return !isEmpty(array);
    }

    public static boolean isNotEmpty(long[] array) {
        return !isEmpty(array);
    }

    public static boolean isNotEmpty(float[] array) {
        return !isEmpty(array);
    }

    public static boolean isNotEmpty(double[] array) {
        return !isEmpty(array);
    }

    public static boolean isNotEmpty(boolean[] array) {
        return !isEmpty(array);
    }

    public static <T> int length(T[] array) {
        return array == null ? 0 : array.length;
    }

    public static int length(Collection<?> array) {
        return array == null ? 0 : array.size();
    }

    public static int length(Map<?, ?> map) {
        return map == null ? 0 : map.size();
    }

    public static int length(byte[] array) {
        return array == null ? 0 : array.length;
    }

    public static int length(char[] array) {
        return array == null ? 0 : array.length;
    }

    public static int length(short[] array) {
        return array == null ? 0 : array.length;
    }

    public static int length(int[] array) {
        return array == null ? 0 : array.length;
    }

    public static int length(long[] array) {
        return array == null ? 0 : array.length;
    }

    public static int length(float[] array) {
        return array == null ? 0 : array.length;
    }

    public static int length(double[] array) {
        return array == null ? 0 : array.length;
    }

    public static int length(boolean[] array) {
        return array == null ? 0 : array.length;
    }

    // ADD

    /**
     * Just concatenates the given elements to the array.
     */
    public static <T> T[] add(T... elements) {
        return elements;
    }

    public static <T> T[] add(T[] array, T element) {
        Class<?> type;
        int length = 0;
        if (array != null) {
            type = array.getClass().getComponentType();
            length = array.length;
        } else if (element != null) {
            type = element.getClass();
        } else {
            throw new IllegalArgumentException("Arguments cannot both be null");
        }
        length += 1;

        ArrayList<T> arr = new ArrayList<>(length);
        if (array != null) {
            Collections.addAll(arr, array);
        }
        Collections.addAll(arr, element);
        return arr.toArray((T[]) Array.newInstance(type, 0));
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
    public static <T> T[] add(T[] array, T... elements) {
        if (array == null) {
            return clone(elements);
        } else if (elements == null) {
            return clone(array);
        }
        ArrayList<T> arr = new ArrayList<>(array.length + elements.length);
        Collections.addAll(arr, array);
        Collections.addAll(arr, elements);
        return arr.toArray((T[]) Array.newInstance(array.getClass().getComponentType(), 0));
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
    public static <T> T[] add(T[]... arrays) {
        if (arrays == null) {
            return null;
        }
        Class<?> type = null;
        ArrayList<T> arr = new ArrayList<>();
        for (T[] el : arrays) {
            if (el != null) {
                Collections.addAll(arr, el);
                type = el.getClass().getComponentType();
            }
        }
        return type == null ? null : arr.toArray((T[]) Array.newInstance(type, 0));
    }

    @Deprecated
    public static <T> T[] addAll(T[] a1, T[] a2) {
        return add(a1, a2);
    }

    // SLICE
    public static <T> T[] slice(T[] values, int startIndexInclusive) {
        return slice(values, startIndexInclusive, Integer.MAX_VALUE);
    }

    public static byte[] slice(byte[] values, int startIndexInclusive) {
        return slice(values, startIndexInclusive, Integer.MAX_VALUE);
    }

    public static char[] slice(char[] values, int startIndexInclusive) {
        return slice(values, startIndexInclusive, Integer.MAX_VALUE);
    }

    public static short[] slice(short[] values, int startIndexInclusive) {
        return slice(values, startIndexInclusive, Integer.MAX_VALUE);
    }

    public static int[] slice(int[] values, int startIndexInclusive) {
        return slice(values, startIndexInclusive, Integer.MAX_VALUE);
    }

    public static long[] slice(long[] values, int startIndexInclusive) {
        return slice(values, startIndexInclusive, Integer.MAX_VALUE);
    }

    public static float[] slice(float[] values, int startIndexInclusive) {
        return slice(values, startIndexInclusive, Integer.MAX_VALUE);
    }

    public static double[] slice(double[] values, int startIndexInclusive) {
        return slice(values, startIndexInclusive, Integer.MAX_VALUE);
    }

    public static boolean[] slice(boolean[] values, int startIndexInclusive) {
        return slice(values, startIndexInclusive, Integer.MAX_VALUE);
    }

    public static <T> T[] slice(T[] values, int startIndexInclusive, int endIndexExclusive) {
        @SuppressWarnings("unchecked") // OK, because array is of type T
        T[] subarray = (T[]) subarray(values, startIndexInclusive, endIndexExclusive);
        return subarray;
    }

    public static byte[] slice(byte[] values, int startIndexInclusive, int endIndexExclusive) {
        @SuppressWarnings("unchecked") // OK, because array is of byte
        byte[] subarray = (byte[]) subarray(values, startIndexInclusive, endIndexExclusive);
        return subarray;
    }

    public static char[] slice(char[] values, int startIndexInclusive, int endIndexExclusive) {
        @SuppressWarnings("unchecked") // OK, because array is of char
        char[] subarray = (char[]) subarray(values, startIndexInclusive, endIndexExclusive);
        return subarray;
    }

    public static short[] slice(short[] values, int startIndexInclusive, int endIndexExclusive) {
        @SuppressWarnings("unchecked") // OK, because array is of short
        short[] subarray = (short[]) subarray(values, startIndexInclusive, endIndexExclusive);
        return subarray;
    }

    public static int[] slice(int[] values, int startIndexInclusive, int endIndexExclusive) {
        @SuppressWarnings("unchecked") // OK, because array is of int
        int[] subarray = (int[]) subarray(values, startIndexInclusive, endIndexExclusive);
        return subarray;
    }

    public static long[] slice(long[] values, int startIndexInclusive, int endIndexExclusive) {
        @SuppressWarnings("unchecked") // OK, because array is of long
        long[] subarray = (long[]) subarray(values, startIndexInclusive, endIndexExclusive);
        return subarray;
    }

    public static float[] slice(float[] values, int startIndexInclusive, int endIndexExclusive) {
        @SuppressWarnings("unchecked") // OK, because array is of float
        float[] subarray = (float[]) subarray(values, startIndexInclusive, endIndexExclusive);
        return subarray;
    }

    public static double[] slice(double[] values, int startIndexInclusive, int endIndexExclusive) {
        @SuppressWarnings("unchecked") // OK, because array is of double
        double[] subarray = (double[]) subarray(values, startIndexInclusive, endIndexExclusive);
        return subarray;
    }

    public static boolean[] slice(boolean[] values, int startIndexInclusive, int endIndexExclusive) {
        @SuppressWarnings("unchecked") // OK, because array is of boolean
        boolean[] subarray = (boolean[]) subarray(values, startIndexInclusive, endIndexExclusive);
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
