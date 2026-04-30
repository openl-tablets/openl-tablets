package org.openl.rules.ruleservice.jaxrs;

import java.util.function.UnaryOperator;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;

import org.openl.rules.openapi.OpenAPIConfiguration;

/**
 * Terminal processor that finalizes the OpenAPI schema by reflecting the JAX-RS resource into the model.
 */
final class FinalizingOpenApiProcessor implements UnaryOperator<OpenAPI> {

    private final Class<?> app;
    private final ObjectMapper mapper;

    FinalizingOpenApiProcessor(Class<?> app, ObjectMapper mapper) {
        this.app = app;
        this.mapper = mapper;
    }

    @Override
    public OpenAPI apply(OpenAPI openAPI) {
        return OpenAPIConfiguration.generateOpenAPI(openAPI, app, mapper);
    }
}
