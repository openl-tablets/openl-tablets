package org.openl.rules.ruleservice.core.interceptors;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Supplier;

public final class AnnotationUtils {
    private AnnotationUtils() {
    }

    public static void inject(Object target,
            Class<? extends Annotation> annotation,
            Supplier<Object> supplier) throws IllegalAccessException, InvocationTargetException {
        if (annotation != null) {
            Class<?> cls = target.getClass();
            Object resource = null;
            boolean initialized = false;
            while (cls != Object.class) {
                for (Field field : cls.getDeclaredFields()) {
                    if (field.isAnnotationPresent(annotation)) {
                        if (!initialized) {
                            resource = supplier.get();
                            if (resource == null) {
                                return;
                            }
                            initialized = true;
                        }
                        if (field.getType().isAssignableFrom(resource.getClass())) {
                            field.setAccessible(true);
                            field.set(target, resource);
                        }
                    }
                }
                cls = cls.getSuperclass();
            }
            for (Method method : target.getClass().getMethods()) {
                if (method.isAnnotationPresent(annotation) && method.getParameterCount() == 1) {
                    if (!initialized) {
                        resource = supplier.get();
                        if (resource == null) {
                            return;
                        }
                        initialized = true;
                    }
                    if (method.getParameterTypes()[0].isAssignableFrom(resource.getClass())) {
                        method.invoke(target, resource);
                    }
                }
            }
        }
    }
}
