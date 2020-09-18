package org.openl.rules.ruleservice.publish.jaxrs.swagger;

import javax.xml.bind.annotation.XmlSeeAlso;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

public final class InheritanceFixConverterHelper {

    private InheritanceFixConverterHelper() {
    }

    public static Class<?> extractParentClass(Class<?> clazz, ObjectMapper objectMapper) {
        if (clazz != null) {
            Class<?> parentClass = null;
            Class<?> baseClass = clazz;
            while (baseClass.getSuperclass() != null && baseClass.getSuperclass() != Object.class) {
                final BeanDescription beanDesc = objectMapper.getSerializationConfig()
                        .introspect(TypeFactory.defaultInstance().constructType(baseClass.getSuperclass()));
                JsonSubTypes jsonSubTypes = beanDesc.getClassAnnotations().get(JsonSubTypes.class);
                if (jsonSubTypes != null && jsonSubTypes.value().length > 0) {
                    parentClass = baseClass.getSuperclass();
                }
                baseClass = baseClass.getSuperclass();
            }
            if (parentClass != null) {
                return parentClass;
            }
            baseClass = clazz;
            while (baseClass.getSuperclass() != null && baseClass.getSuperclass() != Object.class) {
                final BeanDescription beanDesc = objectMapper.getSerializationConfig()
                        .introspect(TypeFactory.defaultInstance().constructType(baseClass.getSuperclass()));
                XmlSeeAlso xmlSeeAlso = beanDesc.getClassAnnotations().get(XmlSeeAlso.class);
                if (xmlSeeAlso != null && xmlSeeAlso.value().length > 0) {
                    parentClass = baseClass.getSuperclass();
                }
                baseClass = baseClass.getSuperclass();
            }
            return parentClass;
        }
        return null;
    }
}
