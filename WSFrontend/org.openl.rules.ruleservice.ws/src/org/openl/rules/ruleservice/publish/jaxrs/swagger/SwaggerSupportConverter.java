package org.openl.rules.ruleservice.publish.jaxrs.swagger;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.bind.annotation.XmlTransient;

import org.apache.commons.lang3.StringUtils;
import org.openl.util.ClassUtils;
import org.openl.util.JAXBUtils;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

import io.swagger.converter.ModelConverter;
import io.swagger.converter.ModelConverterContext;
import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.properties.ObjectProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.StringProperty;

public class SwaggerSupportConverter implements ModelConverter {

    private static final Set<Class<?>> INTERFACES_TO_OBJECT = Collections
        .unmodifiableSet(new HashSet<>(Arrays.asList(Serializable.class, Comparable.class, Cloneable.class)));

    private final ObjectMapper objectMapper;

    public SwaggerSupportConverter(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper cannot be null");
    }

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

        if (model instanceof ModelImpl && t != null) {
            ModelImpl impl = (ModelImpl) model;
            if (StringUtils.isNotBlank(impl.getDiscriminator()) && (impl
                .getProperties() == null || !impl.getProperties().containsKey(impl.getDiscriminator()))) {
                boolean f;
                if (t.getSuperclass() == null) {
                    f = true;
                } else {
                    final BeanDescription superBeanDesc = objectMapper.getSerializationConfig()
                        .introspect(TypeFactory.defaultInstance().constructType(t.getSuperclass()));
                    JsonSubTypes jsonSubTypes = superBeanDesc.getClassInfo().getAnnotation(JsonSubTypes.class);
                    f = jsonSubTypes == null;
                    XmlSeeAlso xmlSeeAlso = superBeanDesc.getClassInfo().getAnnotation(XmlSeeAlso.class);
                    if (xmlSeeAlso != null) {
                        f = false;
                    }
                }
                if (f) {
                    StringProperty discProp = new StringProperty();
                    discProp.setName(impl.getDiscriminator());
                    discProp.setRequired(true);
                    impl.addProperty(impl.getDiscriminator(), discProp);
                }
            }
            final BeanDescription beanDesc = objectMapper.getSerializationConfig()
                .introspect(TypeFactory.defaultInstance().constructType(t));
            JsonSubTypes jsonSubTypes = beanDesc.getClassInfo().getAnnotation(JsonSubTypes.class);
            XmlSeeAlso xmlSeeAlso = beanDesc.getClassInfo().getAnnotation(XmlSeeAlso.class);
            if (jsonSubTypes != null || xmlSeeAlso != null) {
                ObjectProperty objectProperty = new ObjectProperty();
                objectProperty.setType(null);
                impl.setAdditionalProperties(objectProperty);
            }
        }

        if (model != null && model.getProperties() != null && t != null) {
            List<Method> methods = SupportConverterHelper.getAllMethods(t);
            Set<String> methodNames = methods.stream().map(Method::getName).collect(Collectors.toSet());
            for (Method m : methods) {
                if (m.getName().startsWith("get") || m.getName().startsWith("is")) {
                    Property prop = model.getProperties().get(m.getName());
                    if (prop != null) {
                        String getterMethod = ClassUtils.getter(prop.getName());
                        if (!methodNames.contains(getterMethod)) {
                            XmlAttribute xmlAttributeAnn = m.getAnnotation(XmlAttribute.class);
                            if (xmlAttributeAnn != null && !"".equals(xmlAttributeAnn.name()) && !"##default"
                                .equals(xmlAttributeAnn.name())) {
                                prop = prop.rename(xmlAttributeAnn.name());
                            }
                            XmlElement xmlElementAnn = m.getAnnotation(XmlElement.class);
                            if (xmlElementAnn != null && !"".equals(xmlElementAnn.name()) && !"##default"
                                .equals(xmlElementAnn.name())) {
                                prop = prop.rename(xmlElementAnn.name());
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
            } else if (INTERFACES_TO_OBJECT.stream().anyMatch(e -> e == javaType.getRawClass())) {
                return context.resolveProperty(Object.class, annotations);
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
