package org.openl.rules.project.validation.openapi;

import java.lang.reflect.Type;
import java.util.List;

import org.openl.rules.ruleservice.publish.jaxrs.swagger.OpenApiObjectMapperHack;
import org.openl.rules.ruleservice.publish.jaxrs.swagger.OpenApiRulesCacheWorkaround;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContextImpl;
import io.swagger.v3.oas.models.media.Schema;

final class SchemaResolver {
    private SchemaResolver() {
    }

    @SuppressWarnings("rawtypes")
    public static Schema resolve(Context context, Type type) {
        if (type == null) {
            type = Object.class;
        }
        ModelConverterContextImpl modelConverterContext;
        synchronized (OpenApiRulesCacheWorkaround.class) {
            OpenApiObjectMapperHack openApiObjectMapperHack = new OpenApiObjectMapperHack();
            try {
                openApiObjectMapperHack.apply(context.getObjectMapper());
                List<ModelConverter> converters = openApiObjectMapperHack.getModelConverters();
                modelConverterContext = new ModelConverterContextImpl(converters);
            } finally {
                openApiObjectMapperHack.revert();
            }
        }
        return modelConverterContext.resolve(new AnnotatedType().type(type));
    }
}
