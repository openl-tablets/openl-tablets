package org.openl.rules.ruleservice.publish.jaxrs.swagger;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.databind.JavaType;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.oas.models.media.Schema;

import org.openl.util.ClassUtils;
import org.openl.util.JAXBUtils;

public class OpenApiSupportConverter implements ModelConverter {
    public Schema<?> resolve(AnnotatedType annotatedType,
                             ModelConverterContext context,
                             Iterator<ModelConverter> chain) {
        if (annotatedType.getCtxAnnotations() != null) {
            if (Arrays.stream(annotatedType.getCtxAnnotations()).anyMatch(e -> e instanceof XmlTransient)) {
                return null;
            }
        }
        Class<?> valueType = null;
        if (annotatedType.getType() instanceof JavaType) {
            JavaType javaType = (JavaType) annotatedType.getType();
            valueType = JAXBUtils.extractValueTypeIfAnnotatedWithXmlJavaTypeAdapter(javaType.getRawClass());
        } else if (annotatedType.getType() instanceof Class) {
            valueType = JAXBUtils.extractValueTypeIfAnnotatedWithXmlJavaTypeAdapter((Class<?>) annotatedType.getType());
        } else if (annotatedType.getType() == null) {
            valueType = Object.class;
        }
        if (valueType != null) {
            Type oldType = annotatedType.getType();
            try {
                annotatedType.setType(valueType);
                return chain.next().resolve(annotatedType, context, chain);
            } finally {
                annotatedType.setType(oldType);
            }
        }
        return modifySchemaWithAnnotatedType(annotatedType,
                chain.next().resolve(annotatedType, context, chain),
                context);
    }

    private Schema<?> modifySchemaWithAnnotatedType(AnnotatedType annotatedType,
                                                    Schema<?> returnedSchema,
                                                    ModelConverterContext context) {
        Schema<?> schema = returnedSchema;
        if (schema != null && schema.getName() == null) {
            schema = context.resolve(annotatedType);
        }
        if (schema != null) {
            if (schema.getProperties() != null) {
                Class<?> t = null;
                if (annotatedType.getType() instanceof JavaType) {
                    JavaType javaType = (JavaType) annotatedType.getType();
                    t = javaType.getRawClass();
                } else if (annotatedType.getType() instanceof Class) {
                    t = (Class<?>) annotatedType.getType();
                }
                if (t != null) {
                    List<Method> methods = SupportConverterHelper.getAllMethods(t);
                    Set<String> methodNames = methods.stream().map(Method::getName).collect(Collectors.toSet());
                    for (Method m : methods) {
                        if (m.getName().startsWith("get") || m.getName().startsWith("is")) {
                            Schema<?> prop = schema.getProperties().get(m.getName());
                            if (prop != null) {
                                String customPropertyName = null;
                                XmlAttribute xmlAttributeAnn = m.getAnnotation(XmlAttribute.class);
                                if (xmlAttributeAnn != null && !"".equals(xmlAttributeAnn.name()) && !"##default"
                                        .equals(xmlAttributeAnn.name())) {
                                    customPropertyName = xmlAttributeAnn.name();
                                }
                                XmlElement xmlElementAnn = m.getAnnotation(XmlElement.class);
                                if (xmlElementAnn != null && !"".equals(xmlElementAnn.name()) && !"##default"
                                        .equals(xmlElementAnn.name())) {
                                    customPropertyName = xmlElementAnn.name();
                                }
                                String getterMethod = ClassUtils.getter(prop.getName());
                                if (!methodNames.contains(getterMethod) && customPropertyName != null) {
                                    prop.setName(customPropertyName);
                                    schema.getProperties().remove(m.getName());
                                    schema.getProperties().put(prop.getName(), prop);
                                } else if (customPropertyName == null) {
                                    if (prop.getName().startsWith("get")) {
                                        prop.setName(prop.getName().substring(3));
                                    }
                                    if (prop.getName().startsWith("is")) {
                                        prop.setName(prop.getName().substring(2));
                                    }
                                    schema.getProperties().remove(m.getName());
                                    schema.getProperties().put(prop.getName(), prop);
                                }
                            }
                        }
                    }
                }
            }
        }
        return returnedSchema;
    }

}
