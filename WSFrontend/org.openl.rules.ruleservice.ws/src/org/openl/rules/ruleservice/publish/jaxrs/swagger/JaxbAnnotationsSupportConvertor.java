package org.openl.rules.ruleservice.publish.jaxrs.swagger;

import java.lang.annotation.Annotation;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Iterator;

import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.fasterxml.jackson.databind.type.SimpleType;

import io.swagger.converter.ModelConverter;
import io.swagger.converter.ModelConverterContext;
import io.swagger.models.Model;
import io.swagger.models.properties.Property;

public class JaxbAnnotationsSupportConvertor implements ModelConverter {

    private static Class<?> extractValueTypeIfAnnotatedWithXmlJavaTypeAdapter(Class<?> boundType) {
        if (!boundType.isPrimitive()) {
            XmlJavaTypeAdapter xmlJavaTypeAdapter = boundType.getAnnotation(XmlJavaTypeAdapter.class);
            if (xmlJavaTypeAdapter != null) {
                @SuppressWarnings("rawtypes")
                Class<? extends XmlAdapter> xmlAdapterClass = xmlJavaTypeAdapter.value();
                java.lang.reflect.Type type = xmlAdapterClass.getGenericSuperclass();
                if (type instanceof ParameterizedType) {
                    ParameterizedType parameterizedType = (ParameterizedType) type;
                    java.lang.reflect.Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
                    if (actualTypeArguments[0] instanceof Class) {
                        return (Class<?>) actualTypeArguments[0];
                    }
                }
            }
        }
        return null;
    }

    @Override
    public Model resolve(Type type, ModelConverterContext context, Iterator<ModelConverter> chain) {
        if (type instanceof SimpleType) {
            SimpleType simpleType = (SimpleType) type;
            Class<?> valueType = extractValueTypeIfAnnotatedWithXmlJavaTypeAdapter(simpleType.getRawClass());
            if (valueType != null) {
                return context.resolve(valueType);
            }
        }
        return chain.next().resolve(type, context, chain);
    }

    @Override
    public Property resolveProperty(Type type,
            ModelConverterContext context,
            Annotation[] annotations,
            Iterator<ModelConverter> chain) {
        if (annotations != null) {
            for (Annotation annotation : annotations) {
                if (annotation instanceof XmlTransient) {
                    return null;
                }
            }
        }
        if (type instanceof SimpleType) {
            SimpleType simpleType = (SimpleType) type;
            Class<?> valueType = extractValueTypeIfAnnotatedWithXmlJavaTypeAdapter(simpleType.getRawClass());
            if (valueType != null) {
                return context.resolveProperty(valueType, annotations);
            }
        }
        if (type == null) {
            return null;
        }
        return chain.next().resolveProperty(type, context, annotations, chain);
    }
}
