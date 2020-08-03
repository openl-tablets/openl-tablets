package org.open.rules.project.validation.openapi;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.xml.bind.annotation.XmlElement;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.util.ClassUtils;

import com.fasterxml.jackson.annotation.JsonProperty;

class OpenClassPropertiesResolver {
    private final Context context;
    private final Map<IOpenClass, Map<String, IOpenField>> cache = new HashMap<>();

    public OpenClassPropertiesResolver(Context context) {
        this.context = Objects.requireNonNull(context, "context cannot be null");
    }

    private void getFieldAnnotations(Class<?> clazz,
            String fieldName,
            Map<Class<?>, Annotation> annotationsMap,
            boolean isMixInClass) {
        while (clazz != Object.class && clazz != null) {
            if (!isMixInClass) {
                Class<?> mixInClass = context.getObjectMapper().findMixInClassFor(clazz);
                if (mixInClass != null) {
                    getFieldAnnotations(mixInClass, fieldName, annotationsMap, true);
                }
            }
            try {
                Field field = clazz.getDeclaredField(fieldName);
                extractAnnotation(field, XmlElement.class, annotationsMap);
                extractAnnotation(field, JsonProperty.class, annotationsMap);
                if (!isMixInClass) {
                    break;
                }
            } catch (NoSuchFieldException ignore) {
            }
            clazz = clazz.getSuperclass();
        }
    }

    private void getMethodAnnotations(Class<?> clazz,
            String getterName,
            Map<Class<?>, Annotation> annotationsMap,
            boolean isMixInClass) {
        Set<Class<?>> interfaces = new LinkedHashSet<>();
        while (clazz != Object.class && clazz != null) {
            if (!isMixInClass) {
                Class<?> mixInClass = context.getObjectMapper().findMixInClassFor(clazz);
                if (mixInClass != null) {
                    getMethodAnnotations(mixInClass, getterName, annotationsMap, true);
                }
            }
            try {
                Method method = clazz.getMethod(getterName);
                extractAnnotation(method, XmlElement.class, annotationsMap);
                extractAnnotation(method, JsonProperty.class, annotationsMap);
            } catch (NoSuchMethodException ignore) {
            }
            interfaces.addAll(Arrays.asList(clazz.getInterfaces()));
            clazz = clazz.getSuperclass();
        }
        for (Class<?> interfaceClass : interfaces) {
            getMethodAnnotations(interfaceClass, getterName, annotationsMap, false);
        }
    }

    private void extractAnnotation(AnnotatedElement annotatedElement,
            Class<? extends Annotation> annotationClass,
            Map<Class<?>, Annotation> annotationMap) {
        Annotation annotation = annotatedElement.getAnnotation(annotationClass);
        if (annotation != null) {
            annotationMap.put(annotationClass, annotation);
        }
    }

    private IOpenField resolveOpenFieldByPropertyName(IOpenClass openClass, String propertyName) {
        for (IOpenField openField : openClass.getFields()) {
            Map<Class<?>, Annotation> annotationsMap = new HashMap<>();
            getFieldAnnotations(openClass.getInstanceClass(), openField.getName(), annotationsMap, false);
            if (isMatchToPropertyName(annotationsMap, propertyName)) {
                return openField;
            }
            annotationsMap = new HashMap<>();
            String getterName = ClassUtils.getter(openField.getName());
            getMethodAnnotations(openClass.getInstanceClass(), getterName, annotationsMap, false);
            if (isMatchToPropertyName(annotationsMap, propertyName)) {
                return openField;
            }
        }
        return openClass.getField(propertyName);
    }

    private boolean isMatchToPropertyName(Map<Class<?>, Annotation> annotationsMap, String propertyName) {
        JsonProperty jsonProperty = (JsonProperty) annotationsMap.get(JsonProperty.class);
        XmlElement xmlElement = (XmlElement) annotationsMap.get(XmlElement.class);
        if (jsonProperty != null) {
            return Objects.equals(propertyName, jsonProperty.value());
        } else if (xmlElement != null) {
            return Objects.equals(propertyName, xmlElement.name());
        }
        return false;
    }

    public IOpenField getField(IOpenClass openClass, String propertyName) {
        Map<String, IOpenField> propertiesCache = cache.computeIfAbsent(openClass, e -> new HashMap<>());
        return propertiesCache.computeIfAbsent(propertyName, e -> resolveOpenFieldByPropertyName(openClass, e));
    }
}
