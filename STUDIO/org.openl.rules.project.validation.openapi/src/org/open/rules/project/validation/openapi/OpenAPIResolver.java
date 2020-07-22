package org.open.rules.project.validation.openapi;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import org.apache.commons.jxpath.CompiledExpression;
import org.apache.commons.jxpath.JXPathContext;
import org.apache.commons.jxpath.JXPathException;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ComposedSchema;
import io.swagger.v3.oas.models.media.Schema;

final class OpenAPIResolver {
    private final Context context;
    private final JXPathContext jxPathContext;
    private final Map<String, Object> resolvedByRefCache = new HashMap<>();
    @SuppressWarnings("rawtypes")
    private final Map<Schema, Map<String, Schema>> allPropertiesCache = new IdentityHashMap<>();

    public OpenAPIResolver(Context context, OpenAPI openAPI) {
        this.context = Objects.requireNonNull(context, "context cannot be null");
        Objects.requireNonNull(openAPI, "openAPI cannot be null");
        this.jxPathContext = JXPathContext.newContext(openAPI);
    }

    @SuppressWarnings("unchecked")
    public <T> T resolve(T obj, Function<T, String> getRefFunc) {
        if (obj != null && getRefFunc.apply(obj) != null) {
            return resolve((T) resolveByRef(getRefFunc.apply(obj)), getRefFunc);
        }
        return obj;
    }

    private Object resolveByRef(String ref) {
        if (resolvedByRefCache.containsKey(ref)) {
            return resolvedByRefCache.get(ref);
        }
        CompiledExpression compiledExpression = JXPathContext.compile(ref.substring(1));
        try {
            Object resolvedByRef = compiledExpression.createPath(jxPathContext).getValue();
            resolvedByRefCache.put(ref, resolvedByRef);
            return resolvedByRef;
        } catch (JXPathException e) {
            resolvedByRefCache.put(ref, null);
            if (context.isTypeValidationInProgress()) {
                OpenApiProjectValidatorMessagesUtils.addTypeError(context,
                    String.format("Invalid $ref '%s' is used in the OpenAPI file.", ref));
            } else {
                OpenApiProjectValidatorMessagesUtils.addMethodError(context,
                    String.format("Invalid $ref '%s' is used in the OpenAPI file.", ref));
            }
            return null;
        }
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
            } else {
                if (resolvedSchema.getProperties() != null) {
                    allSchemaProperties.putAll(resolvedSchema.getProperties());
                }
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
