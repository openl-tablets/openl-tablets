package org.openl.rules.ruleservice.jaxrs;

import java.util.function.UnaryOperator;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.SneakyThrows;

/**
 * Optional processor that applies the bundled {@code openapi-security.json} overlay when authentication is enabled.
 */
final class SecurityOpenApiProcessor implements UnaryOperator<OpenAPI> {

    private final ObjectMapper mapper;

    SecurityOpenApiProcessor(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @SneakyThrows
    public OpenAPI apply(OpenAPI openAPI) {
        return mapper.readerForUpdating(openAPI).readValue(getClass().getResourceAsStream("/openapi-security.json"));
    }
}
