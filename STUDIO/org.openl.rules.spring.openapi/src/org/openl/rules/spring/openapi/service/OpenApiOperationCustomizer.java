package org.openl.rules.spring.openapi.service;

import io.swagger.v3.oas.models.Operation;
import org.jspecify.annotations.NonNull;

import org.openl.rules.spring.openapi.model.MethodInfo;

/**
 * Adds cross-cutting touches to generated OpenAPI {@link Operation}s.
 *
 * <p>Implementations are optional Spring beans. The reader applies every customizer bean to every
 * generated operation. When no implementation is registered, operation generation is unchanged.
 *
 * <p>This is how a feature (for example, response field projection) advertises a global query
 * parameter only when its own bean is wired into the application.
 *
 * @author Vladyslav Pikus
 */
public interface OpenApiOperationCustomizer {

    /**
     * Modifies the given operation in place.
     *
     * @param methodInfo the handler method the operation was generated from
     * @param operation  the operation to customize
     */
    void customize(@NonNull MethodInfo methodInfo, @NonNull Operation operation);
}
