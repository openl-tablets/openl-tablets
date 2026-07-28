package org.openl.rules.ruleservice.jaxrs;

import java.util.function.UnaryOperator;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import org.openl.rules.openapi.OpenAPIConfiguration;

/**
 * Terminal processor that finalizes the OpenAPI schema by reflecting the JAX-RS resource into the model.
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
final class FinalizingOpenApiProcessor implements UnaryOperator<OpenAPI> {

    private final Class<?> app;
    private final ObjectMapper mapper;

    @Override
    public OpenAPI apply(OpenAPI openAPI) {
        return OpenAPIConfiguration.generateOpenAPI(openAPI, app, mapper);
    }
}
