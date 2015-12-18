package org.openl.extension.xmlrules.utils;

import java.lang.reflect.Array;

public class HelperFunctions {
    public static <T> T[][] transpose(T[][] arr) {
        if (arr.length == 0) {
            return arr;
        }

        Class clazz = arr[0].getClass().getComponentType();
        @SuppressWarnings("unchecked")
        T[][] newArr = (T[][]) Array.newInstance(clazz, arr[0].length, arr.length);
        for (int i = 0; i < arr.length; i++) {
            T[] row = arr[i];
            for (int j = 0; j < row.length; j++) {
                newArr[j][i] = row[j];
            }
        }
        return newArr;
    }
}
