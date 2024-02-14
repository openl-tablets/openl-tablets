package org.openl.rules.openapi;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Schema;

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
                    .collect(Collectors.toList())) {
                if (resolvedByRef != null) {
                    try {
                        resolvedByRef = ClassUtils.get(resolvedByRef, expressionPart);
                    } catch (Exception e) {
                        if (Map.class.isAssignableFrom(resolvedByRef.getClass())) {
                            resolvedByRef = ((Map<?, ?>) resolvedByRef).get(expressionPart);
                        } else {
                            resolvedByRef = null;
                        }
                    }
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

    public Map<String, Schema> resolveAllProperties(Schema<?> schema, Map<Schema, Map<String, Schema>> allPropertiesCache) {
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
                        Map<String, Schema> embeddedSchemaProperties = resolveAllProperties(embeddedSchema, allPropertiesCache);
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
