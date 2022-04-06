package org.openl.rules.spring.openapi.model;

import io.swagger.v3.oas.models.responses.ApiResponses;

/**
 * Information holder for beans annotated with {@link org.springframework.web.bind.annotation.ControllerAdvice}
 */
public class ControllerAdviceInfo {

    private final Object controllerAdvice;
    private final ApiResponses apiResponseMap = new ApiResponses();

    public ControllerAdviceInfo(Object controllerAdvice) {
        this.controllerAdvice = controllerAdvice;
    }

    public Object getControllerAdvice() {
        return controllerAdvice;
    }

    public ApiResponses getApiResponses() {
        return apiResponseMap;
    }
}
