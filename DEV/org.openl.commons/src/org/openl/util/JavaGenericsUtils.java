package org.openl.util;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

public final class JavaGenericsUtils {
    private JavaGenericsUtils() {
    }

    public static String getGenericTypeName(Type type) {
        if (type instanceof TypeVariable) {
            @SuppressWarnings("rawtypes")
            TypeVariable typeVariable = (TypeVariable) type;
            return typeVariable.getName();
        }
        if (type instanceof GenericArrayType) {
            GenericArrayType genericArrayType = (GenericArrayType) type;
            return getGenericTypeName(genericArrayType.getGenericComponentType());
        }
        return null;
    }

    public static int getGenericTypeDim(Type type) {
        if (type instanceof TypeVariable) {
            return 0;
        }
        if (type instanceof GenericArrayType) {
            GenericArrayType genericArrayType = (GenericArrayType) type;
            return 1 + getGenericTypeDim(genericArrayType.getGenericComponentType());
        }
        return -1;
    }
}
