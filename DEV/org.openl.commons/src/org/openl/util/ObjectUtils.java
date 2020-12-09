package org.openl.util;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Set of utilities for manipulating with objects.
 *
 * @author Yury Molchan
 */
public class ObjectUtils {

    /**
     * Converts a string to the value of the given type.
     * 
     * @param value an input string for conversion to an object value.
     * @param type a type of the result.
     * @return an object value of the given type.
     */
    public static Object convert(String value, Class<?> type) {
        if (value == null) {
            return null;
        }
        if (type.isEnum()) {
            return Enum.valueOf((Class<Enum>) type, value);
        } else if (type.isAssignableFrom(String.class)) {
            return value;
        } else if (type.isArray()) {
            final Class<?> componentType = type.getComponentType();
            String[] values = value.split(",");
            Object res = Array.newInstance(componentType, values.length);
            for (int i = 0; i < values.length; i++) {
                Array.set(res, i, convert(values[i], componentType));
            }
            return res;
        } else {
            if (type.isPrimitive()) {
                type = ClassUtils.primitiveToWrapper(type);
            }
            try {
                try {
                    Method method = type.getDeclaredMethod("valueOf", String.class);
                    return method.invoke(null, value);
                } catch (NoSuchMethodException e) {
                    try {
                        Method method = type.getDeclaredMethod("parse", CharSequence.class);
                        return method.invoke(null, value);
                    } catch (NoSuchMethodException e1) {
                        try {
                            Constructor<?> constructor = type.getDeclaredConstructor(String.class);
                            return constructor.newInstance(value);
                        } catch (NoSuchMethodException e2) {
                            throw new IllegalArgumentException(String.format(
                                "Neither public constructor '%s(String s)', nor public static method 'valueOf(String s)', nor public static method 'parse(CharSequence s)' is not found.",
                                type.getTypeName()), e2);
                        }
                    }
                }
            } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
                throw new IllegalArgumentException(
                    String.format("Cannot convert '%s' string to '%s' type", value, type.getTypeName()),
                    e);
            }
        }
    }
}
