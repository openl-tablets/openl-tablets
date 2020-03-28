package org.openl.rules.ruleservice.publish.jaxrs.swagger;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import org.openl.util.ClassUtils;
import org.openl.util.JAXBUtils;

import com.fasterxml.jackson.databind.JavaType;

import io.swagger.converter.ModelConverter;
import io.swagger.converter.ModelConverterContext;
import io.swagger.models.Model;
import io.swagger.models.properties.Property;

public class SwaggerSupportConverter implements ModelConverter {

    @Override
    public Model resolve(Type type, ModelConverterContext context, Iterator<ModelConverter> chain) {
        Class<?> t;
        Model model;
        if (type instanceof JavaType) {
            JavaType javaType = (JavaType) type;
            Class<?> valueType = JAXBUtils.extractValueTypeIfAnnotatedWithXmlJavaTypeAdapter(javaType.getRawClass());
            if (valueType != null) {
                model = context.resolve(valueType);
                t = valueType;
            } else {
                model = chain.next().resolve(type, context, chain);
                t = javaType.getRawClass();
            }
        } else if (type instanceof Class) {
            Class<?> clazz = (Class<?>) type;
            Class<?> valueType = JAXBUtils.extractValueTypeIfAnnotatedWithXmlJavaTypeAdapter(clazz);
            if (valueType != null) {
                model = context.resolve(valueType);
                t = valueType;
            } else {
                model = chain.next().resolve(type, context, chain);
                t = clazz;
            }
        } else {
            model = chain.next().resolve(type, context, chain);
            t = null;
        }
        if (model != null && model.getProperties() != null && t != null) {
            List<Method> methods = new ArrayList<>();
            while (!Object.class.equals(t) && t != null) {
                methods.addAll(Arrays.asList(t.getDeclaredMethods()));
                t = t.getSuperclass();
            }
            Set<String> methodNames = methods.stream().map(Method::getName).collect(Collectors.toSet());
            for (Method m : methods) {
                if (m.getName().startsWith("get") || m.getName().startsWith("is")) {
                    Property prop = model.getProperties().get(m.getName());
                    if (prop != null) {
                        String getterMethod = ClassUtils.getter(prop.getName());
                        if (!methodNames.contains(getterMethod)) {
                            XmlAttribute xmlAttributeAnn = m.getAnnotation(XmlAttribute.class);
                            if (xmlAttributeAnn != null) {
                                prop = prop.rename(xmlAttributeAnn.name());
                                prop.setXml(null);
                            }
                            XmlElement xmlElementAnn = m.getAnnotation(XmlElement.class);
                            if (xmlElementAnn != null) {
                                prop = prop.rename(xmlElementAnn.name());
                                prop.setXml(null);
                            }
                            if (xmlElementAnn != null || xmlAttributeAnn != null) {
                                model.getProperties().remove(m.getName());
                                model.getProperties().put(prop.getName(), prop);
                            }
                        }
                    }
                }
            }
        }
        return model;
    }

    @Override
    public Property resolveProperty(Type type,
            ModelConverterContext context,
            Annotation[] annotations,
            Iterator<ModelConverter> chain) {
        if (annotations != null) {
            if (Arrays.stream(annotations).anyMatch(e -> e instanceof XmlTransient)) {
                return null;
            }
        }
        if (type instanceof JavaType) {
            JavaType javaType = (JavaType) type;
            if (java.util.Optional.class.isAssignableFrom(javaType.getRawClass())) {
                if (javaType.containedType(0) == null) {
                    return context.resolveProperty(Object.class, annotations);
                }
            } else {
                Class<?> valueType = JAXBUtils
                    .extractValueTypeIfAnnotatedWithXmlJavaTypeAdapter(javaType.getRawClass());
                if (valueType != null) {
                    return context.resolveProperty(valueType, annotations);
                }
            }
        }
        return chain.next().resolveProperty(type, context, annotations, chain);
    }

}
