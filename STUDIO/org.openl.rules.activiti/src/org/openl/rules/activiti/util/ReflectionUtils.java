package org.openl.rules.activiti.util;

import java.lang.reflect.ParameterizedType;

public final class ReflectionUtils {

    private ReflectionUtils() {
    }

    @SuppressWarnings("rawtypes")
    public static Class getGenericParameterClass(Class actualClass, int parameterIndex) {
        return (Class) ((ParameterizedType) actualClass.getGenericSuperclass())
            .getActualTypeArguments()[parameterIndex];
    }
}
