package org.openl.rules.spring.openapi.service;

import io.swagger.v3.oas.models.Operation;

import org.openl.rules.spring.openapi.model.MethodInfo;

/**
 * SPI for cross-cutting customization of generated OpenAPI {@link Operation}s.
 *
 * <p>Implementations are <em>optional</em> Spring beans. The reader applies every customizer bean present
 * in the context to each generated operation; if no such bean exists, operation generation is unchanged.
 * This lets a feature (e.g. response field projection) advertise a global query parameter only when the
 * corresponding feature bean is actually wired into the application.
 *
 * @author Vladyslav Pikus
 */
public interface OpenApiOperationCustomizer {

    /**
     * Customizes a single generated operation (e.g. adds a cross-cutting query parameter).
     *
     * @param methodInfo the handler method the operation was generated from
     * @param operation  the operation to customize in place
     */
    void customize(MethodInfo methodInfo, Operation operation);
}
