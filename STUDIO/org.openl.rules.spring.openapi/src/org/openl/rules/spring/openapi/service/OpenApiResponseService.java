package org.openl.rules.spring.openapi.service;

import org.openl.rules.spring.openapi.model.ControllerAdviceInfo;
import org.openl.rules.spring.openapi.model.MethodInfo;

import io.swagger.v3.oas.models.responses.ApiResponses;

/**
 * The builder class for OpenAPI responses
 *
 * @author Vladyslav Pikus
 */
public interface OpenApiResponseService {

    /**
     * Generate OpenApi Responses for Spring Controller Advice bean
     *
     * @param apiContext current OpenApi context
     * @param controllerAdviceInfo controller advice to scan
     */
    void generateResponses(OpenApiContext apiContext, ControllerAdviceInfo controllerAdviceInfo);

    /**
     * Build OpenAPI {@link ApiResponses}
     *
     * @param apiContext current OpenAPI context
     * @param methodInfo Spring method handler
     * @return resulted API responses or empty
     */
    ApiResponses generateResponses(OpenApiContext apiContext, MethodInfo methodInfo);

}
