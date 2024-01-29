package org.openl.rules.project.validation.openapi;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Schema;

import org.openl.rules.project.openapi.OpenAPIRefResolver;

final class OpenAPIResolver {
    private final OpenAPIRefResolver openAPIRefResolver;
    private final Context context;

    @SuppressWarnings("rawtypes")
    private final Map<Schema, Map<String, Schema>> allPropertiesCache = new IdentityHashMap<>();

    public OpenAPIResolver(Context context, OpenAPI openAPI) {
        this.context = Objects.requireNonNull(context, "context cannot be null");
        this.openAPIRefResolver = new OpenAPIRefResolver(Objects.requireNonNull(openAPI, "openAPI cannot be null"));
    }

    public <T> T resolve(T obj, Function<T, String> getRefFunc) {
        return openAPIRefResolver.resolve(obj, getRefFunc, () -> {
            if (context.isTypeValidationInProgress()) {
                OpenApiProjectValidatorMessagesUtils.addTypeError(context,
                    String.format("Invalid $ref '%s' is used in the OpenAPI file.", getRefFunc.apply(obj)));
            } else {
                OpenApiProjectValidatorMessagesUtils.addMethodError(context,
                    String.format("Invalid $ref '%s' is used in the OpenAPI file.", getRefFunc.apply(obj)));
            }
        });
    }

    @SuppressWarnings("rawtypes")
    public Map<String, Schema> resolveAllProperties(Schema<?> schema) {
        Map<String, Schema> allSchemaProperties = allPropertiesCache.get(schema);
        if (allSchemaProperties != null) {
            return allSchemaProperties;
        }
        Schema<?> resolvedSchema = resolve(schema, Schema::get$ref);
        if (resolvedSchema != null) {
            allSchemaProperties = new HashMap<>();
            if (resolvedSchema instanceof ComposedSchema) {
                ComposedSchema composedSchema = (ComposedSchema) resolvedSchema;
                if (composedSchema.getAllOf() != null && !composedSchema.getAllOf().isEmpty()) {
                    for (Schema<?> embeddedSchema : composedSchema.getAllOf()) {
                        Map<String, Schema> embeddedSchemaProperties = resolveAllProperties(embeddedSchema);
                        if (embeddedSchemaProperties != null) {
                            allSchemaProperties.putAll(embeddedSchemaProperties);
                        }
                    }
                }
            }
            if (resolvedSchema.getProperties() != null) {
                allSchemaProperties.putAll(resolvedSchema.getProperties());
            }
            if (resolvedSchema != schema) {
                allPropertiesCache.put(resolvedSchema, allSchemaProperties);
            }
        } else {
            allSchemaProperties = schema.getProperties() != null ? schema.getProperties() : Collections.emptyMap();
        }
        allPropertiesCache.put(schema, allSchemaProperties);
        return allSchemaProperties;
    }

}
