package org.openl.rules.ruleservice.publish.jaxrs.swagger;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Iterator;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.SimpleType;

import io.swagger.converter.ModelConverter;
import io.swagger.converter.ModelConverterContext;
import io.swagger.models.Model;
import io.swagger.models.properties.Property;

public class RulesSupportConverter implements ModelConverter {

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
            Class<?> t;
            Model model;
            if (valueType != null) {
                model = context.resolve(valueType);
                t = valueType;
            } else {
                model = chain.next().resolve(type, context, chain);
                t = simpleType.getRawClass();
            }
            if (model != null) {
                // Workaround for bug in ModelResolver
                for (Method m : t.getMethods()) {
                    if (model.getProperties() != null) {
                        Property prop = model.getProperties().get(m.getName());
                        if (prop != null) {
                            XmlAttribute xmlAttributeAnn = m.getAnnotation(XmlAttribute.class);
                            if (xmlAttributeAnn != null) {
                                prop.setName(xmlAttributeAnn.name());
                            }
                            XmlElement xmlElementAnn = m.getAnnotation(XmlElement.class);
                            if (xmlElementAnn != null) {
                                prop.setName(xmlElementAnn.name());
                            }
                            if (xmlElementAnn != null || xmlAttributeAnn != null) {
                                model.getProperties().remove(m.getName());
                                model.getProperties().put(prop.getName(), prop);
                            }
                        }
                    }
                }
            }
            return model;
        } else {
            return chain.next().resolve(type, context, chain);
        }
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
            if (java.util.Optional.class.isAssignableFrom(((SimpleType) type).getRawClass())) {
                if (((JavaType) type).containedType(0) == null) {
                    type = Object.class;
                }
            } else {
                SimpleType simpleType = (SimpleType) type;
                Class<?> valueType = extractValueTypeIfAnnotatedWithXmlJavaTypeAdapter(simpleType.getRawClass());
                if (valueType != null) {
                    return context.resolveProperty(valueType, annotations);
                }
            }
        }
        return chain.next().resolveProperty(type, context, annotations, chain);
    }

}
