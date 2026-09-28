package org.openl.rules.openapi;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Schema;
import org.jspecify.annotations.Nullable;

import org.openl.util.ClassUtils;

public class OpenAPIRefResolver {
    private final Map<String, Object> resolvedByRefCache = new HashMap<>();
    private final OpenAPI openAPI;

    public OpenAPIRefResolver(OpenAPI openAPI) {
        this.openAPI = Objects.requireNonNull(openAPI, "openAPI cannot be null");
    }

    public Object resolve(String ref) {
        return resolveByRef(ref, () -> {
        });
    }

    public <T> T resolve(T obj, Function<T, String> getRefFunc) {
        return resolve(obj, getRefFunc, () -> {
        });
    }

    @SuppressWarnings("unchecked")
    public <T> T resolve(T obj, Function<T, String> getRefFunc, Runnable retNotFoundFunc) {
        if (obj != null && getRefFunc.apply(obj) != null) {
            return resolve((T) resolveByRef(getRefFunc.apply(obj), retNotFoundFunc), getRefFunc);
        }
        return obj;
    }

    private Object resolveByRef(String ref, Runnable retNotFoundFunc) {
        if (resolvedByRefCache.containsKey(ref)) {
            return resolvedByRefCache.get(ref);
        }
        String expression = ref.substring(1);
        String[] expressionParts = expression.split("(?=/)");
        Object resolvedByRef = openAPI;
        try {
            for (String expressionPart : Arrays.stream(expressionParts)
                    .map(e -> e.substring(1))
                    .toList()) {
                if (resolvedByRef != null) {
                    resolvedByRef = resolvePart(resolvedByRef, expressionPart);
                }
            }
        } catch (Exception e) {
            resolvedByRef = null;
        }
        if (resolvedByRef != openAPI && resolvedByRef != null) {
            resolvedByRefCache.put(ref, resolvedByRef);
            return resolvedByRef;
        } else {
            resolvedByRefCache.put(ref, null);
            retNotFoundFunc.run();
            return null;
        }
    }

    private static @Nullable Object resolvePart(Object resolvedByRef, String expressionPart) {
        try {
            return ClassUtils.get(resolvedByRef, expressionPart);
        } catch (Exception e) {
            if (Map.class.isAssignableFrom(resolvedByRef.getClass())) {
                return ((Map<?, ?>) resolvedByRef).get(expressionPart);
            } else {
                return null;
            }
        }
    }

    public Map<String, Schema> resolveAllProperties(Schema<?> schema, Map<Schema, Map<String, Schema>> allPropertiesCache) {
        Map<String, Schema> allSchemaProperties = allPropertiesCache.get(schema);
        if (allSchemaProperties != null) {
            return allSchemaProperties;
        }
        Schema<?> resolvedSchema = resolve(schema, Schema::get$ref);
        if (resolvedSchema != null) {
            allSchemaProperties = collectAllProperties(resolvedSchema, allPropertiesCache);
            if (resolvedSchema != schema) {
                allPropertiesCache.put(resolvedSchema, allSchemaProperties);
            }
        } else {
            allSchemaProperties = schema.getProperties() != null ? schema.getProperties() : Map.of();
        }
        allPropertiesCache.put(schema, allSchemaProperties);
        return allSchemaProperties;
    }

    /**
     * Returns the properties of the schema together with the properties of all schemas it is composed of.
     */
    private Map<String, Schema> collectAllProperties(Schema<?> resolvedSchema,
                                                     Map<Schema, Map<String, Schema>> allPropertiesCache) {
        Map<String, Schema> allSchemaProperties = new HashMap<>();
        if (resolvedSchema instanceof ComposedSchema composedSchema
                && composedSchema.getAllOf() != null && !composedSchema.getAllOf().isEmpty()) {
            for (Schema<?> embeddedSchema : composedSchema.getAllOf()) {
                Map<String, Schema> embeddedSchemaProperties = resolveAllProperties(embeddedSchema, allPropertiesCache);
                if (embeddedSchemaProperties != null) {
                    allSchemaProperties.putAll(embeddedSchemaProperties);
                }
            }
        }
        if (resolvedSchema.getProperties() != null) {
            allSchemaProperties.putAll(resolvedSchema.getProperties());
        }
        return allSchemaProperties;
    }
}
