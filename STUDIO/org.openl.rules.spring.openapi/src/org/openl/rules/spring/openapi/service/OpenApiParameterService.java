package org.openl.rules.spring.openapi.service;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.annotation.JsonView;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.media.Schema;

@Component
public class OpenApiParameterService {

    @Autowired
    public OpenApiParameterService(Optional<List<ModelConverter>> modelConverters) {
        modelConverters.ifPresent(converters -> converters.forEach(ModelConverters.getInstance()::addConverter));
    }

    @SuppressWarnings("rawtypes")
    public Schema resolveSchema(Type type, Components components, JsonView jsonView) {
        var resolvedSchema = ModelConverters.getInstance()
            .resolveAsResolvedSchema(new AnnotatedType(type).resolveAsRef(true).jsonViewAnnotation(jsonView));
        if (resolvedSchema.schema == null) {
            return null;
        }
        if (resolvedSchema.referencedSchemas != null) {
            resolvedSchema.referencedSchemas.forEach(components::addSchemas);
        }
        return resolvedSchema.schema;
    }

}
