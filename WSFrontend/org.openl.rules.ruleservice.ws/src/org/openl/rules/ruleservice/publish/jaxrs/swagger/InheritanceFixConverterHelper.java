package org.openl.rules.ruleservice.publish.jaxrs.swagger;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class InheritanceFixConverterHelper {

    private InheritanceFixConverterHelper() {
    }

    public static Class<?> extractBaseClass(Class<?> clazz, ObjectMapper objectMapper) {
        Class<?> baseClass = clazz;
        boolean f = true;
        while (baseClass.getSuperclass() != null && baseClass.getSuperclass() != Object.class && f) {
            f = false;
            Class<?> superClass = baseClass.getSuperclass();
            JsonSubTypes jsonSubTypes = superClass.getAnnotation(JsonSubTypes.class);

            Class<?> mixInClass = objectMapper.findMixInClassFor(superClass);
            if (mixInClass != null) {
                jsonSubTypes = mixInClass.getAnnotation(JsonSubTypes.class);
            }

            if (jsonSubTypes != null && jsonSubTypes.value().length > 0) {
                f = true;
                baseClass = superClass;
            }
        }
        return baseClass != clazz ? baseClass : null;
    }
}
