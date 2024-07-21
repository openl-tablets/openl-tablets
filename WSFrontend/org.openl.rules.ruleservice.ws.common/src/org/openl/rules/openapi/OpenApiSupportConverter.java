package org.openl.rules.openapi;

import java.util.Arrays;
import java.util.Iterator;
import javax.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.databind.JavaType;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.oas.models.media.Schema;

import org.openl.util.JAXBUtils;

class OpenApiSupportConverter implements ModelConverter {

    static final OpenApiSupportConverter INSTANCE = new OpenApiSupportConverter();

    public Schema<?> resolve(AnnotatedType annotatedType,
                             ModelConverterContext context,
                             Iterator<ModelConverter> chain) {
        if (annotatedType.getCtxAnnotations() != null) {
            // Skip transient fields
            if (Arrays.stream(annotatedType.getCtxAnnotations()).anyMatch(e -> e instanceof XmlTransient)) {
                return null;
            }
        }
        // Replace with JAXB adapter type
        var valueType = annotatedType.getType();
        if (valueType instanceof JavaType) {
            JavaType javaType = (JavaType) valueType;
            valueType = JAXBUtils.extractValueTypeIfAnnotatedWithXmlJavaTypeAdapter(javaType.getRawClass());
        } else if (valueType instanceof Class) {
            valueType = JAXBUtils.extractValueTypeIfAnnotatedWithXmlJavaTypeAdapter((Class<?>) valueType);
        } else if (valueType == null) {
            // If a generic type is not defined, we will assume that it is an Object.
            valueType = Object.class;
        }
        if (valueType != null) {
            annotatedType.setType(valueType);
        }
        return chain.next().resolve(annotatedType, context, chain);
    }
}
