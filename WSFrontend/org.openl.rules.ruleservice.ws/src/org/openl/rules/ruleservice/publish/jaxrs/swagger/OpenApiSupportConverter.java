package org.openl.rules.ruleservice.publish.jaxrs.swagger;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Iterator;

import javax.xml.bind.annotation.XmlTransient;

import org.openl.util.JAXBUtils;

import com.fasterxml.jackson.databind.JavaType;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.oas.models.media.Schema;

public class OpenApiSupportConverter implements ModelConverter {
    public Schema resolve(AnnotatedType annotatedType, ModelConverterContext context, Iterator<ModelConverter> chain) {
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
            Class<?> clazz = (Class<?>) annotatedType.getType();
            valueType = JAXBUtils.extractValueTypeIfAnnotatedWithXmlJavaTypeAdapter(clazz);
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
        return chain.next().resolve(annotatedType, context, chain);
    }
}
