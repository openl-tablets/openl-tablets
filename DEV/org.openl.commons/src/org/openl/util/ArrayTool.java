package org.openl.util;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.ClassUtils;

public class ArrayTool {

    static class ArrayIterator<T> implements Iterator<T> {
        int _index = 0;
        int _size;
        T[] _array;

        ArrayIterator(T[] array) {
            _size = Array.getLength(array);
            _array = array;
        }

        public boolean hasNext() {
            return _index < _size;
        }

        public T next() {
            return _array[_index++];
        }

        public void remove() {
            throw new UnsupportedOperationException("Should not be called");
        }

    }

    public static final Object[] ZERO_OBJECT = {};

    public static String asString(Object ary) {
        return asString(ary, 128);
    }

    public static String asString(Object ary, int maxLength) {
        StringBuilder buf = new StringBuilder(100);

        print(ary, buf, maxLength);

        if (buf.length() > maxLength) {
            String ellipses = "...";
            buf.delete(maxLength - ellipses.length(), buf.length()).append(ellipses);
        }

        return buf.toString();
    }

    public static boolean contains(Object array, Object test) {
        return findFirstElementIndex(array, test) >= 0;
    }

    /**
     * Returns true if array container contains all the elements of array
     * testArray
     */

    public static <T> boolean containsAll(T[] container, T[] testArray) {
        if (container == null || testArray == null) {
            return false;
        }
        Iterator<T> it = iterator(testArray);
        while (it.hasNext()) {
            if (!contains(container, it.next())) {
                return false;
            }
        }
        return true;
    }

    public static int findFirstElementIndex(Object array, Object element) {
        return findFirstIndex(array, ASelector.selectObject(element));
    }

    public static int findFirstIndex(Object array, ISelector<Object> sel) {
        int size = Array.getLength(array);
        for (int i = 0; i < size; ++i) {
            if (sel.select(Array.get(array, i))) {
                return i;
            }
        }

        return -1;
    }

    public static Object insertValue(int i, Object oldArray, Object value) {
        int oldSize = Array.getLength(oldArray);
        Object newArray = Array.newInstance(oldArray.getClass().getComponentType(), oldSize + 1);

        if (i > 0) {
            System.arraycopy(oldArray, 0, newArray, 0, i);
        }

        Array.set(newArray, i, value);

        if (i < oldSize) {
            System.arraycopy(oldArray, i, newArray, i + 1, oldSize - i);
        }

        return newArray;
    }

    public static <T> Iterator<T> iterator(T[] array) {
        return new ArrayIterator<T>(array);
    }

    public static Object merge(Object array1, Object array2) {
        return merge(new Object[] { array1, array2 });
    }

    public static Object merge(Object[] arrays) {
        if (arrays == null || arrays.length == 0) {
            return new Object[0];
        }
        if (arrays.length == 1) {
            return arrays[0];
        }

        int newSize = 0;
        for (int i = 0; i < arrays.length; i++) {
            newSize += Array.getLength(arrays[i]);
        }

        if (newSize == Array.getLength(arrays[0])) {
            return arrays[0];
        }

        Object newArray = Array.newInstance(arrays[0].getClass().getComponentType(), newSize);
        int pos = 0;
        for (int i = 0; i < arrays.length; i++) {
            int sz = Array.getLength(arrays[i]);
            System.arraycopy(arrays[i], 0, newArray, pos, sz);
            pos += Array.getLength(arrays[i]);
        }
        return newArray;
    }

    static void print(Object obj, StringBuilder buf, int maxLength) {
        if (obj == null) {
            buf.append("null");
        } else if (obj instanceof String) {
            buf.append('"').append(obj).append('"');
        } else if (obj instanceof Class<?>) {
            buf.append(((Class<?>) obj).getName());
        } else if (obj.getClass().isArray()) {
            printArray(obj, buf, maxLength);
        } else {
            buf.append(obj);
        }
    }

    static void printArray(Object ary, StringBuilder buf, int maxLength) {
        int size = Array.getLength(ary);

        buf.append('[');
        for (int i = 0; i < size; ++i) {
            if (i > 0) {
                buf.append(", ");
            }

            print(Array.get(ary, i), buf, maxLength);

            if (buf.length() > maxLength) {
                return;
            }
        }
        buf.append(']');
    }

    public static Object removeValue(int i, Object oldArray) {
        int oldSize = Array.getLength(oldArray);

        Object newArray = Array.newInstance(oldArray.getClass().getComponentType(), oldSize - 1);

        if (i > 0) {
            System.arraycopy(oldArray, 0, newArray, 0, i);
        }

        int i1 = i + 1;

        if (i1 < oldSize) {
            System.arraycopy(oldArray, i1, newArray, i, oldSize - i1);
        }

        return newArray;
    }

    public static Object replace(int index, Object oldArray, Object newValue) {
        Object newArray = removeValue(index, oldArray);
        return insertValue(index, newArray, newValue);
    }

    public static <T> Object[] toArray(List<T> v, Class<?> c) {
        Object[] ary = (Object[]) Array.newInstance(c, v.size());
        return v.toArray(ary);
    }

    /**
     * Checks that array is not empty.
     * 
     * Used the following rules for checking: isEmpty (null) -> true; isEmpty
     * (new Object[] {}) -> true; isEmpty (new Object[] {null, null, ....}) ->
     * true; isEmpty (new Object[] {null, ..., <not null value>, ...}) -> false.
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
        List<String> v = new ArrayList<String>();
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
     * <li>if object is array - result is array of objects. If input array is
     * array of primitive types - result is array of appropriate wrapper types.
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

    public static boolean noNulls(Object[] values) {
        boolean result = false;
        if (ArrayUtils.isNotEmpty(values)) {
            for (Object value : values) {
                if (value == null) {
                    return result;
                }
            }
            result = true;
        }
        return result;
    }

    public static int getNotNullValuesCount(Object[] values) {
        if (ArrayUtils.isEmpty(values)) {
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

    public static byte[] sort(byte[] values) {
        if (values != null) {
            Arrays.sort(values);
        }
        return values;
    }

    public static short[] sort(short[] values) {
        if (values != null) {
            Arrays.sort(values);
        }
        return values;
    }

    public static int[] sort(int[] values) {
        if (values != null) {
            Arrays.sort(values);
        }
        return values;
    }

    public static long[] sort(long[] values) {
        if (values != null) {
            Arrays.sort(values);
        }
        return values;
    }

    public static float[] sort(float[] values) {
        if (values != null) {
            Arrays.sort(values);
        }
        return values;
    }

    public static double[] sort(double[] values) {
        if (values != null) {
            Arrays.sort(values);
        }
        return values;
    }

    public static Object[] sort(Object[] values) {
        Object[] sortedArray = null;
        if (isArrayNull(values)) {
            sortedArray = new Object[values.length];
            Object[] notNullArray = ArrayTool.removeNulls(values);
            Arrays.sort(notNullArray);

            /* Filling sortedArray by sorted and null values */
            for (int i = 0; i < notNullArray.length; i++) {
                sortedArray[i] = notNullArray[i];
            }
        }
        return sortedArray;
    }

    public static String[] sort(String[] values) {
        String[] sortedArray = null;

        if (values != null) {
            sortedArray = new String[values.length];
            String[] notNullArray = ArrayTool.removeNulls(values);
            Arrays.sort(notNullArray);

            /* Filling sortedArray by sorted and null values */
            for (int i = 0; i < notNullArray.length; i++) {
                sortedArray[i] = notNullArray[i];
            }
        }
        return sortedArray;
    }

    public static Date[] sort(Date[] values) {
        Date[] sortedArray = null;

        if (isArrayNull(values)) {
            sortedArray = new Date[values.length];
            Date[] notNullArray = ArrayTool.removeNulls(values);
            Arrays.sort(notNullArray);

            /* Filling sortedArray by sorted and null values */
            for (int i = 0; i < notNullArray.length; i++) {
                sortedArray[i] = notNullArray[i];
            }
        }
        return sortedArray;
    }

    private static boolean isArrayNull(Object[] values) {
        if (values != null) {
            return true;
        } else {
            return false;
        }
    }

    public static Class<?> getLowerComponentType(Class<?> clazz) {
        if (clazz.isArray()) {
            return getLowerComponentType(clazz.getComponentType());
        }
        return clazz;
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
