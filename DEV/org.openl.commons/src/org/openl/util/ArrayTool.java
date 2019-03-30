package org.openl.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ArrayTool {

    static class ArrayIterator<T> implements Iterator<T> {
        int _index = 0;
        int _size;
        T[] _array;

        ArrayIterator(T[] array) {
            _size = Array.getLength(array);
            _array = array;
        }

        @Override
        public boolean hasNext() {
            return _index < _size;
        }

        @Override
        public T next() {
            return _array[_index++];
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Should not be called");
        }

    }

    public static final Object[] ZERO_OBJECT = {};

    public static boolean contains(Object array, Object test) {
        int size = Array.getLength(array);
        for (int i = 0; i < size; ++i) {
            if (ASelector.selectObject(test).select(Array.get(array, i))) {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns true if array container contains all the elements of array testArray
     */

    public static <T> boolean containsAll(T[] container, T[] testArray) {
        if (container == null || testArray == null) {
            return false;
        }
        Iterator<T> it = new ArrayIterator<T>(testArray);
        while (it.hasNext()) {
            if (!contains(container, it.next())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks that array is not empty.
     * 
     * Used the following rules for checking: isEmpty (null) -> true; isEmpty (new Object[] {}) -> true; isEmpty (new
     * Object[] {null, null, ....}) -> true; isEmpty (new Object[] {null, ..., <not null value>, ...}) -> false.
     * 
     * @param array array
     * @return true if array is empty; false - otherwise
     */
    public static boolean isEmpty(Object[] array) {

        if (array != null) {

            for (Object element : array) {
                if (element != null) {
                    return false;
                }
            }
        }

        return true;
    }

    public static boolean containsAll(int[] ary1, int[] ary2) {
        if (ary1 == null || ary2 == null) {
            return false;
        }

        for (int arrayElement : ary2) {
            if (!contains(ary1, arrayElement)) {
                return false;
            }
        }
        return true;
    }

    public static boolean containsAll(byte[] ary1, byte[] ary2) {
        if (ary1 == null || ary2 == null) {
            return false;
        }

        for (byte arrayElement : ary2) {
            if (!contains(ary1, arrayElement)) {
                return false;
            }
        }
        return true;
    }

    public static boolean containsAll(short[] ary1, short[] ary2) {
        if (ary1 == null || ary2 == null) {
            return false;
        }

        for (short arrayElement : ary2) {
            if (!contains(ary1, arrayElement)) {
                return false;
            }
        }
        return true;
    }

    public static boolean containsAll(long[] ary1, long[] ary2) {
        if (ary1 == null || ary2 == null) {
            return false;
        }

        for (long arrayElement : ary2) {
            if (!contains(ary1, arrayElement)) {
                return false;
            }
        }
        return true;
    }

    public static boolean containsAll(char[] ary1, char[] ary2) {
        if (ary1 == null || ary2 == null) {
            return false;
        }

        for (char arrayElement : ary2) {
            if (!contains(ary1, arrayElement)) {
                return false;
            }
        }
        return true;
    }

    public static boolean containsAll(float[] ary1, float[] ary2) {
        if (ary1 == null || ary2 == null) {
            return false;
        }

        for (float arrayElement : ary2) {
            if (!contains(ary1, arrayElement)) {
                return false;
            }
        }
        return true;
    }

    public static boolean containsAll(String[] ary1, String[] ary2) {
        if (ary1 == null || ary2 == null) {
            return false;
        }

        for (String arrayElement : ary2) {
            if (!contains(ary1, arrayElement)) {
                return false;
            }
        }
        return true;
    }

    public static boolean containsAll(double[] ary1, double[] ary2) {
        if (ary1 == null || ary2 == null) {
            return false;
        }

        for (double arrayElement : ary2) {
            if (!contains(ary1, arrayElement)) {
                return false;
            }
        }
        return true;
    }

    public static boolean containsAll(boolean[] ary1, boolean[] ary2) {
        if (ary1 == null || ary2 == null) {
            return false;
        }
        for (boolean arrayElement : ary2) {
            if (!contains(ary1, arrayElement)) {
                return false;
            }
        }
        return true;
    }

    public static String[] intersection(String[] ary1, String[] ary2) {
        List<String> v = new ArrayList<>();
        for (int j = 0; j < ary2.length; ++j) {
            if (contains(ary1, ary2[j])) {
                v.add(ary2[j]);
            }
        }
        return v.toArray(new String[v.size()]);
    }

    /**
     * Converts given object to array object. Used the following rules:
     * <ul>
     * <li>if object is null - result is null
     * <li>if object is array - result is array of objects. If input array is array of primitive types - result is array
     * of appropriate wrapper types.
     * <li>if object is not array - result is array with one element
     * </ul>
     * 
     * 
     * @param object input object
     * @return array of objects
     */
    public static Object[] toArray(Object object) {

        if (object == null) {
            return null;
        }

        int size;

        if (!object.getClass().isArray()) {
            size = 1;
        } else {
            size = Array.getLength(object);
        }

        Class<?> clazz;

        if (object.getClass().isArray()) {
            clazz = object.getClass().getComponentType();
        } else {
            clazz = object.getClass();
        }

        Class<?> componentType;

        if (clazz.isPrimitive()) {
            componentType = ClassUtils.primitiveToWrapper(clazz);
        } else {
            componentType = clazz;
        }

        Object[] newArray = (Object[]) Array.newInstance(componentType, size);

        for (int i = 0; i < size; i++) {
            newArray[i] = Array.get(object, i);
        }

        return newArray;
    }

    public static int getNotNullValuesCount(Object[] values) {
        if (CollectionUtils.isEmpty(values)) {
            return 0;
        }

        int count = values.length;

        for (Object value : values) {
            if (value == null) {
                count--;
            }
        }

        return count;
    }

    @SuppressWarnings("unchecked")
    public static <T> T[] removeNulls(T[] array) {
        T[] result;

        int valuableSize = getNotNullValuesCount(array);

        if (array == null || valuableSize == array.length) {
            result = array;
        } else {
            result = (T[]) Array.newInstance(array.getClass().getComponentType(), valuableSize);

            int i = 0;
            for (T value : array) {
                if (value != null) {
                    result[i] = value;
                    i++;
                }
            }
        }
        return result;
    }

}
