package org.openl.rules.spring.openapi.service;

import java.util.List;
import java.util.Optional;

import org.openl.rules.spring.openapi.OpenApiContext;
import org.openl.rules.spring.openapi.model.MethodInfo;
import org.openl.rules.spring.openapi.model.ParameterInfo;
import org.springframework.stereotype.Component;

import io.swagger.v3.oas.models.parameters.RequestBody;

@Component
public class OpenApiRequestService {

    private final OpenApiParameterService apiParameterService;

    public OpenApiRequestService(OpenApiParameterService apiParameterService) {
        this.apiParameterService = apiParameterService;
    }

    public Optional<RequestBody> parse(OpenApiContext apiContext,
            MethodInfo methodInfo,
            List<ParameterInfo> bodyParams) {
        return Optional.empty();
    }
}
