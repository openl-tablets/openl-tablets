/*
 * Created on May 21, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.util;

import java.lang.reflect.Array;

/**
 * @author snshor
 *
 * This class contains a bunch of utilities related to arrays, Iterarators and
 * Collections. Hence, the name
 */

public class CollectionsUtil {
    public static Object add(Object array, Object value) {
        return insertValue(Array.getLength(array), array, value);
    }

    /**
     * Collect objects from array src[] to array dst[] using Collector ic. Does
     * not check boundaries.
     *
     * @param dst
     * @param src
     * @param ic
     * @return array dst[]
     */

    static public <D, S> Object[] collect(D[] dst, S[] src, IConvertor<S, D> ic) {
        for (int i = 0; i < src.length; i++) {
            dst[i] = ic.convert(src[i]);
        }
        return dst;
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

}
