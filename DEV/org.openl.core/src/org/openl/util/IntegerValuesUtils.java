package org.openl.util;

import org.openl.meta.IntValue;
import org.openl.meta.LongValue;
import org.openl.meta.ShortValue;

public class IntegerValuesUtils {
    public static boolean isIntegerValue(Class<?> clazz) {
        return byte.class.equals(clazz)
                || short.class.equals(clazz)
                || int.class.equals(clazz)
                || long.class.equals(clazz)
                || Byte.class.equals(clazz)
                || Short.class.equals(clazz)
                || Integer.class.equals(clazz)
                || Long.class.equals(clazz)
                || ShortValue.class.equals(clazz)
                || IntValue.class.equals(clazz)
                || LongValue.class.equals(clazz);
    }
    
    public static Object createNewObjectByType(Class<?> clazz, String value) {
        if (byte.class.equals(clazz)) {
            return Byte.valueOf(value);
        } else if (short.class.equals(clazz)) {
            return Short.valueOf(value);
        } else if (int.class.equals(clazz)) {
            return Integer.valueOf(value);
        } else if (long.class.equals(clazz)) {
            return Long.valueOf(value);
        } else if (Byte.class.equals(clazz)) {
            return Byte.valueOf(value);
        } else if (Short.class.equals(clazz)) {
            return Short.valueOf(value);
        } else if (Integer.class.equals(clazz)) {
            return Integer.valueOf(value);
        } else if (Long.class.equals(clazz)) {
            return Long.valueOf(value);
        } else if (ShortValue.class.equals(clazz)) {
            return new org.openl.meta.ShortValue((Short.valueOf(value)));
        } else if (IntValue.class.equals(clazz)) {
            return new org.openl.meta.IntValue(Integer.valueOf(value));
        } else if (LongValue.class.equals(clazz)) {
            return new org.openl.meta.LongValue((Long.valueOf(value)));
        }

        return null;
    }
}
