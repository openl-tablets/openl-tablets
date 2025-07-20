package org.openl.rules.spring.openapi.service;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import org.openl.util.RuntimeExceptionWrapper;

public class OpenApiServiceImpl implements OpenApiService {

    private final ApplicationContext context;
    private final OpenApiSpringMvcReader openApiSpringMvcReader;
    private volatile OpenAPI calculatedOpenApi;

    public OpenApiServiceImpl(ApplicationContext context,
                              OpenApiSpringMvcReader openApiSpringMvcReader) {
        this.context = context;
        this.openApiSpringMvcReader = openApiSpringMvcReader;
    }

    private OpenAPI calculateOpenApi() {
        var openApiContext = new OpenApiContext();

        Map<String, Object> controllers = new HashMap<>();
        controllers.putAll(context.getBeansWithAnnotation(RestController.class));
        controllers.putAll(context.getBeansWithAnnotation(ResponseBody.class));

        Map<String, Class<?>> filteredControllers = controllers.entrySet()
                .stream()
                .filter(e -> AnnotationUtils.findAnnotation(e.getValue().getClass(), Hidden.class) == null)
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getClass(), (a1, a2) -> a1));

        openApiSpringMvcReader.read(openApiContext, filteredControllers);
        try {
            return fromJson(asJson(openApiContext.getOpenAPI()));
        } catch (JsonProcessingException e) {
            throw RuntimeExceptionWrapper.wrap("Failed to copy calculated OpenAPI schema", e);
        }
    }

    private static String asJson(OpenAPI openAPI) throws JsonProcessingException {
        return Json.mapper().writeValueAsString(openAPI);
    }

    private static OpenAPI fromJson(String json) throws JsonProcessingException {
        return Json.mapper().readValue(json, OpenAPI.class);
    }

    /**
     * Gets calculated OpenAPI schema
     *
     * @return calculated OpenAPI schema
     */
    @Override
    public String getCalculatedOpenApi() {
        if (this.calculatedOpenApi == null) {
            synchronized (this) {
                if (this.calculatedOpenApi == null) {
                    this.calculatedOpenApi = calculateOpenApi();
                }
            }
        }
        try {
            return asJson(this.calculatedOpenApi);
        } catch (JsonProcessingException e) {
            throw RuntimeExceptionWrapper.wrap("Failed to copy calculated OpenAPI schema", e);
        }
    }
}
