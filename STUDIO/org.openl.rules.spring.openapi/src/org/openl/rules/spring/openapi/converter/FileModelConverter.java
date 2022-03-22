package org.openl.rules.spring.openapi.converter;

import java.util.Iterator;

import org.openl.rules.spring.openapi.OpenApiUtils;
import org.springframework.stereotype.Component;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.media.FileSchema;
import io.swagger.v3.oas.models.media.Schema;

@Component
public class FileModelConverter implements ModelConverter {

    @Override
    @SuppressWarnings("rawtypes")
    public Schema resolve(AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
        var javaType = Json.mapper().constructType(type.getType());
        if (javaType != null && OpenApiUtils.isFile(javaType.getRawClass())) {
            return new FileSchema();
        }
        return (chain.hasNext()) ? chain.next().resolve(type, context, chain) : null;
    }
}
