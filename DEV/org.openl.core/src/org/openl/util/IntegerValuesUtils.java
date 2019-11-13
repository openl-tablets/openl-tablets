package org.openl.util;

import java.math.BigInteger;

import org.openl.meta.*;

public final class IntegerValuesUtils {

    private IntegerValuesUtils() {
    }

    public static boolean isIntegerValue(Class<?> clazz) {
        return byte.class == clazz || short.class == clazz || int.class == clazz || long.class == clazz || Byte.class == clazz || Short.class == clazz || Integer.class == clazz || Long.class == clazz || BigInteger.class == clazz || ByteValue.class == clazz || ShortValue.class == clazz || IntValue.class == clazz || LongValue.class == clazz || BigIntegerValue.class == clazz;
    }

    public static Object createNewObjectByType(Class<?> clazz, String value) {
        if (byte.class == clazz) {
            return Byte.valueOf(value);
        } else if (short.class == clazz) {
            return Short.valueOf(value);
        } else if (int.class == clazz) {
            return Integer.valueOf(value);
        } else if (long.class == clazz) {
            return Long.valueOf(value);
        } else if (Byte.class == clazz) {
            return Byte.valueOf(value);
        } else if (Short.class == clazz) {
            return Short.valueOf(value);
        } else if (Integer.class == clazz) {
            return Integer.valueOf(value);
        } else if (Long.class == clazz) {
            return Long.valueOf(value);
        } else if (BigInteger.class == clazz) {
            return new BigInteger(value);
        } else if (ByteValue.class == clazz) {
            return new org.openl.meta.ByteValue(value);
        } else if (ShortValue.class == clazz) {
            return new org.openl.meta.ShortValue(Short.valueOf(value));
        } else if (IntValue.class == clazz) {
            return new org.openl.meta.IntValue(Integer.valueOf(value));
        } else if (LongValue.class == clazz) {
            return new org.openl.meta.LongValue(Long.valueOf(value));
        } else if (BigIntegerValue.class == clazz) {
            return new BigIntegerValue(value);
        }

        return null;
    }
}
