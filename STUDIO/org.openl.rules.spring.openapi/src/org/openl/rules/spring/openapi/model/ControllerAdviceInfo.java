package org.openl.rules.spring.openapi.model;

import io.swagger.v3.oas.models.responses.ApiResponses;
import lombok.RequiredArgsConstructor;

/**
 * Information holder for beans annotated with {@link org.springframework.web.bind.annotation.ControllerAdvice}
 */
@RequiredArgsConstructor
public class ControllerAdviceInfo {

    private final Object controllerAdvice;
    private final ApiResponses apiResponseMap = new ApiResponses();

    public Object getControllerAdvice() {
        return controllerAdvice;
    }

    public ApiResponses getApiResponses() {
        return apiResponseMap;
    }
}
