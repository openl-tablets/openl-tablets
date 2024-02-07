package org.openl.rules.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Comparator;

import org.openl.binding.impl.method.NonNullLiteral;

/**
 * A set of util methods to work with arrays.
 * <p>
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
     * Arrays.addElement(null, 0, null)      = [null]
     * Arrays.addElement(null, 0, "a")       = ["a"]
     * Arrays.addElement(["a"], 1, null)     = ["a", null]
     * Arrays.addElement(["a"], 1, "b")      = ["a", "b"]
     * Arrays.addElement(["a", "b"], 3, "c") = ["a", "b", "c"]
     * </pre>
     *
     * @param array    the array to add the element to, may be <code>null</code>
     * @param index    the position of the new object
     * @param elements the objects to add
     * @return A new array containing the existing elements and the new element
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index > array.length).
     */
    public static <T> T[] addElement(T[] array, int index, @NonNullLiteral T... elements) {
        if (elements == null) {
            return array;
        }
        if (array == null) {
            return elements;
        }
        Class<?> componentType;
        Class<?> arrayComponentType = array.getClass().getComponentType();
        Class<?> elementsComponentType = elements.getClass().getComponentType();
        if (arrayComponentType.isAssignableFrom(elementsComponentType)) {
            componentType = arrayComponentType;
        } else if (elementsComponentType.isAssignableFrom(arrayComponentType)) {
            componentType = elementsComponentType;
        } else {
            componentType = Object.class;
        }
        Object[] result = (Object[]) Array.newInstance(componentType, array.length + elements.length);
        System.arraycopy(array, 0, result, 0, index);
        System.arraycopy(elements, 0, result, index, elements.length);
        System.arraycopy(array, index, result, index + elements.length, array.length - index);

        return (T[]) result;
    }

    public static <T> T[] addElement(T[] array, int index, T element) {
        if (array == null && element == null) {
            return null;
        }
        Object oneElementArray = Array
                .newInstance(element != null ? element.getClass() : array.getClass().getComponentType(), 1);
        if (element != null) {
            Array.set(oneElementArray, 0, element);
        }
        return addElement(array, index, (T[]) oneElementArray);
    }

    // SLICE
    public static <T> T[] slice(T[] values, int startIndexInclusive) {
        return slice(values, startIndexInclusive, Integer.MAX_VALUE);
    }

    public static <T> T[] slice(T[] values, int startIndexInclusive, int endIndexExclusive) {
        if (isEmpty(values)) {
            return values;
        }
        // handle negatives
        int size = values.length;
        if (endIndexExclusive < 0) {
            endIndexExclusive = size + endIndexExclusive; // remember end is negative
        }
        if (startIndexInclusive < 0) {
            startIndexInclusive = size + startIndexInclusive; // remember start is negative

        }

        // check length next
        if (endIndexExclusive > size) {
            endIndexExclusive = size;
        }

        if (startIndexInclusive < 0) {
            startIndexInclusive = 0;
        }
        if (endIndexExclusive < 0) {
            endIndexExclusive = 0;
        }

        final int newSize = endIndexExclusive - startIndexInclusive;
        if (newSize == size && startIndexInclusive == 0) {
            return values;
        }

        if (newSize <= 0) {
            return (T[]) Array.newInstance(values.getClass().getComponentType(), 0);
        } else {
            return java.util.Arrays.copyOfRange(values, startIndexInclusive, endIndexExclusive);
        }
    }

    /**
     * Removes the first occurrence of the specified element from the specified array. All subsequent elements are
     * shifted to the left (substracts one from their indices). If the array does not contains such an element, no
     * elements are removed from the array. <br />
     * <br />
     * <p/>
     * This method returns a new array with the same elements of the input array except the first occurrence of the
     * specified element. The component type of the returned array is always the same as that of the input array. <br />
     * <br />
     * <p/>
     * <code>
     * ArrayUtils.removeElement(null, "a")            = null        <br />
     * ArrayUtils.removeElement([], "a")              = []          <br />
     * ArrayUtils.removeElement(["a"], "b")           = ["a"]       <br />
     * ArrayUtils.removeElement(["a", "b"], "a")      = ["b"]       <br />
     * ArrayUtils.removeElement(["a", "b", "a"], "a") = ["b", "a"]  <br />
     * </code>
     *
     * @param array    the array to remove the element from, may be null
     * @param elements
     * @return the element to be removed
     */
    public static <T, E extends T> T[] removeElement(T[] array, E... elements) {
        if (array == null) {
            return null;
        }
        if (elements == null) {
            return array;
        }
        ArrayList<T> result = new ArrayList<>(java.util.Arrays.asList(array));
        java.util.Arrays.stream(elements).forEach(result::remove);
        return (T[]) result.toArray();
    }

    /**
     * Return a new array without null elements
     *
     * @param elements whose null elements should be removed
     * @return array without null elements
     */
    public static <T> T[] removeNulls(T... elements) {
        if (isEmpty(elements)) {
            return elements;
        }

        // Count non-nulls elements to create an array with appropriate size.
        int count = 0;
        for (T value : elements) {
            if (value != null) {
                count++;
            }
        }

        if (count == elements.length) {
            // No null elements, so return the same array.
            return elements;
        }

        // Copy non-null elements to the result array.
        Class<?> componentType = elements.getClass().getComponentType();
        T[] result = (T[]) Array.newInstance(componentType, count);
        int i = 0;
        for (T value : elements) {
            if (value != null) {
                result[i] = value;
                i++;
            }
        }
        return result;
    }

    /**
     * Returns {@code true} if the input elements exist, and they do not contain {@code null} elements.
     */
    public static <T> boolean noNulls(T... values) {
        if (isEmpty(values)) {
            return false;
        }
        for (T item : values) {
            if (item == null) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns a copy of the specified array into ascending order, according to the {@linkplain Comparable natural
     * ordering} of its elements. All elements in the array must implement the {@link Comparable} interface.
     * Furthermore, all elements in the array must be <i>mutually comparable</i> (that is, {@code e1.compareTo(e2)} must
     * not throw a {@code ClassCastException} for any elements {@code e1} and {@code e2} in the array).
     * <p/>
     * {@code null} values are kept in the tail.
     * <p/>
     * <p/>
     * This sort is guaranteed to be <i>stable</i>: equal elements will not be reordered as a result of the sort.
     *
     * @param values the array to be sorted
     * @return a sorted array
     */
    @SuppressWarnings("unchecked")
    public static <T extends Comparable<T>> T[] sort(T... values) {
        if (isEmpty(values)) {
            return values;
        }
        T[] sortedArray = values.clone();
        java.util.Arrays.sort(sortedArray, Comparator.nullsLast(Comparator.naturalOrder()));
        return sortedArray;
    }

}
