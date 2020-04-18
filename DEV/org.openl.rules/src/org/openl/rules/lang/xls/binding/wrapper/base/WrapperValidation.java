package org.openl.rules.lang.xls.binding.wrapper.base;

import java.lang.reflect.Modifier;
import java.util.Arrays;

public final class WrapperValidation {
    private WrapperValidation() {
    }

    public static void validateWrapperClass(Class<?> methodWrapperClass, Class<?> methodClass) {
        if (Arrays.stream(methodClass.getDeclaredMethods())
            .filter(
                e -> !e.isSynthetic() && Modifier.isPublic(e.getModifiers()) && !Modifier.isStatic(e.getModifiers()))
            .anyMatch(e -> {
                try {
                    methodWrapperClass.getDeclaredMethod(e.getName(), e.getParameterTypes());
                } catch (NoSuchMethodException ignore) {
                    return true;
                }
                return false;
            })) {
            throw new IllegalStateException(String.format("%s must override all public methods of %s",
                methodWrapperClass.getTypeName(),
                methodClass.getTypeName()));
        }
    }
}
