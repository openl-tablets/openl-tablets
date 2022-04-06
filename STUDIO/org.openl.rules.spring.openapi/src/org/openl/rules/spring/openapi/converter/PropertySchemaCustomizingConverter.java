package org.openl.rules.spring.openapi.converter;

import java.util.Iterator;

import org.openl.rules.spring.openapi.service.OpenApiPropertyResolver;
import org.openl.util.StringUtils;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import io.swagger.v3.core.converter.AnnotatedType;
import io.swagger.v3.core.converter.ModelConverter;
import io.swagger.v3.core.converter.ModelConverterContext;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.models.media.Schema;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PropertySchemaCustomizingConverter implements ModelConverter {

    private final OpenApiPropertyResolver apiPropertyResolver;

    public PropertySchemaCustomizingConverter(OpenApiPropertyResolver apiPropertyResolver) {
        this.apiPropertyResolver = apiPropertyResolver;
    }

    @Override
    @SuppressWarnings("rawtypes")
    public Schema resolve(AnnotatedType type, ModelConverterContext context, Iterator<ModelConverter> chain) {
        if (chain.hasNext()) {
            var resolvedSchema = chain.next().resolve(type, context, chain);
            if (resolvedSchema == null) {
                return null;
            }
            if (type.isSchemaProperty()) {
                if (type.getCtxAnnotations() != null) {
                    for (var anno : type.getCtxAnnotations()) {
                        if (anno.annotationType() == Deprecated.class) {
                            resolvedSchema.setDeprecated(Boolean.TRUE);
                        } else if (anno instanceof Parameter) {
                            // Support Parameter when it's defined on class field. Swagger doesn't support this case
                            var paramApi = (Parameter) anno;
                            if (StringUtils.isNotBlank(paramApi.description())) {
                                resolvedSchema.setDescription(paramApi.description());
                            }
                            if (StringUtils.isNotBlank(paramApi.example())) {
                                resolvedSchema.setExample(paramApi.example());
                            }
                            if (paramApi.required()) {
                                type.getParent().addRequiredItem(type.getPropertyName());
                            }
                        }
                    }
                }
            }
            if (StringUtils.isNotBlank(resolvedSchema.getDescription())) {
                resolvedSchema.setDescription(apiPropertyResolver.resolve(resolvedSchema.getDescription()));
            }
            return resolvedSchema;
        }
        return null;
    }
}
