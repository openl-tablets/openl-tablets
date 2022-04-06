package org.openl.rules.spring.openapi.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.openl.rules.spring.openapi.OpenApiContext;
import org.openl.util.RuntimeExceptionWrapper;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.models.OpenAPI;

public class OpenApiServiceImpl {

    private final ApplicationContext context;
    private final OpenApiSpringMvcReaderImpl openApiSpringMvcReader;
    private final Set<Class<?>> ignoreControllers;
    private volatile OpenAPI calculatedOpenApi;

    public OpenApiServiceImpl(ApplicationContext context,
            OpenApiSpringMvcReaderImpl openApiSpringMvcReader,
            Set<Class<?>> ignoreControllers) {
        this.context = context;
        this.ignoreControllers = ignoreControllers;
        this.openApiSpringMvcReader = openApiSpringMvcReader;
    }

    public synchronized void build() {
        var openApiContext = new OpenApiContext();

        Map<String, Object> controllers = new HashMap<>();
        controllers.putAll(context.getBeansWithAnnotation(RestController.class));
        controllers.putAll(context.getBeansWithAnnotation(ResponseBody.class));

        Map<String, Class<?>> filteredControllers = controllers.entrySet()
            .stream()
            .filter(e -> AnnotationUtils.findAnnotation(e.getValue().getClass(), Hidden.class) == null)
            .filter(e -> !ignoreControllers.contains(e.getValue().getClass()))
            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().getClass(), (a1, a2) -> a1));

        openApiSpringMvcReader.read(openApiContext, filteredControllers);
        try {
            calculatedOpenApi = fromJson(asJson(openApiContext.getOpenAPI()));
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

    public String getOpenApiAsJson() throws JsonProcessingException {
        return asJson(calculatedOpenApi);
    }
}
