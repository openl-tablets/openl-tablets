package org.openl.rules.util;

import java.lang.reflect.Array;
import java.util.Collection;
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
}
