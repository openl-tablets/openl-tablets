package org.openl.rules.spring.openapi.service;

import java.util.List;

import org.openl.rules.spring.openapi.model.MethodInfo;
import org.openl.rules.spring.openapi.model.ParameterInfo;

import io.swagger.v3.oas.models.parameters.RequestBody;

/**
 * OpenAPI RequestBody service helps to parse and build OpenAPI request bodies from API annotation and Spring
 * declaration
 *
 * @author Vladyslav Pikus
 */
public interface OpenApiRequestService {

    /**
     * Generates RequestBody from request body parameter or creates request body if requested media type is form
     *
     * @param apiContext curren OpenAPI context
     * @param methodInfo controller method info
     * @param formParameters the list of form parameters
     * @param requestBodyParam request body or empty
     * @return resolved request body or empty
     */
    RequestBody generateRequestBody(OpenApiContext apiContext,
            MethodInfo methodInfo,
            List<ParameterInfo> formParameters,
            ParameterInfo requestBodyParam);

    /**
     * Merges source RequestBody to target
     * 
     * @param target target request body
     * @param source source request body
     */
    void mergeRequestBody(RequestBody source, RequestBody target);

    /**
     * Check if current parameter is RequestBody
     *
     * @param paramInfo method parameter
     * @return is request body or not
     */
    boolean isRequestBody(ParameterInfo paramInfo);

}
