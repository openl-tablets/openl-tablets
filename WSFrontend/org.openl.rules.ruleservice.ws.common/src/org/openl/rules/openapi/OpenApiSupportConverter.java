package org.openl.rules.openapi;

import java.util.Arrays;
import java.util.Iterator;
import jakarta.xml.bind.annotation.XmlTransient;

import com.fasterxml.jackson.databind.JavaType;
import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.oas.models.media.Schema;

import org.openl.util.JAXBUtils;

class OpenApiSupportConverter implements ModelConverter {

    static final OpenApiSupportConverter INSTANCE = new OpenApiSupportConverter();

    @Override
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
        switch (valueType) {
            case null -> valueType = Object.class;
            case JavaType javaType -> valueType = JAXBUtils.extractValueTypeIfAnnotatedWithXmlJavaTypeAdapter(javaType.getRawClass());
            case Class<?> class1 -> valueType = JAXBUtils.extractValueTypeIfAnnotatedWithXmlJavaTypeAdapter(class1);
            default -> {}
        }
        if (valueType != null) {
            annotatedType.setType(valueType);
        }
        return chain.next().resolve(annotatedType, context, chain);
    }
}
