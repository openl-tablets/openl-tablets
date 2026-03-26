package org.openl.rules.project.validation.openapi;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;

import org.openl.rules.openapi.OpenAPIRefResolver;


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
                        "Invalid $ref '%s' is used in the OpenAPI file.".formatted(getRefFunc.apply(obj)));
            } else {
                OpenApiProjectValidatorMessagesUtils.addMethodError(context,
                        "Invalid $ref '%s' is used in the OpenAPI file.".formatted(getRefFunc.apply(obj)));
            }
        });
    }

    @SuppressWarnings("rawtypes")
    public Map<String, Schema> resolveAllProperties(Schema<?> schema) {
        return openAPIRefResolver.resolveAllProperties(schema, allPropertiesCache);
    }

}
