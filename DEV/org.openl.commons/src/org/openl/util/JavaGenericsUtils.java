package org.openl.util;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

public final class JavaGenericsUtils {
    private JavaGenericsUtils() {
    }

    public static String getGenericTypeName(Type type) {
        if (type instanceof TypeVariable typeVariable) {
            return typeVariable.getName();
        }
        if (type instanceof GenericArrayType genericArrayType) {
            return getGenericTypeName(genericArrayType.getGenericComponentType());
        }
        return null;
    }

    public static int getGenericTypeDim(Type type) {
        if (type instanceof TypeVariable) {
            return 0;
        }
        if (type instanceof GenericArrayType genericArrayType) {
            return 1 + getGenericTypeDim(genericArrayType.getGenericComponentType());
        }
        return -1;
    }
}
