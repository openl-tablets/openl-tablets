package org.openl.rules.ruleservice.core.interceptors;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Function;

public final class AnnotationUtils {
    private AnnotationUtils() {
    }

    public static Object inject(Object target,
            Class<? extends Annotation> annotationClass,
            Function<Annotation, Object> supplier) throws IllegalAccessException, InvocationTargetException {
        if (annotationClass != null) {
            Class<?> cls = target.getClass();
            Object resource = null;
            boolean initialized = false;
            while (cls != Object.class) {
                for (Field field : cls.getDeclaredFields()) {
                    Annotation annotation = field.getAnnotation(annotationClass);
                    if (annotation != null) {
                        if (!initialized) {
                            resource = supplier.apply(annotation);
                            if (resource == null) {
                                return null;
                            }
                            initialized = true;
                        }
                        field.setAccessible(true);
                        field.set(target, resource);
                    }
                }
                cls = cls.getSuperclass();
            }
            for (Method method : target.getClass().getMethods()) {
                if (method.getParameterCount() == 1) {
                    Annotation annotation = method.getAnnotation(annotationClass);
                    if (annotation != null) {
                        if (!initialized) {
                            resource = supplier.apply(annotation);
                            if (resource == null) {
                                return null;
                            }
                            initialized = true;
                        }
                        method.invoke(target, resource);
                    }
                }
            }
            return resource;
        }
        return null;
    }
}
