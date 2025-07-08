package org.openl.rules.spring.openapi.controller;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import org.openl.rules.spring.openapi.service.OpenApiServiceImpl;

/**
 * OpenAPI Controller
 */
@Controller
@RequestMapping("/api-docs")
public class OpenApiController {

    private final OpenApiServiceImpl openApiService;

    public OpenApiController(OpenApiServiceImpl openApiService) {
        this.openApiService = openApiService;
    }

    /**
     * Gets generated OpenAPI schema as JSON string
     *
     * @return json string
     */
    @GetMapping(value = "/openapi.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String openApi() {
        return openApiService.getCalculatedOpenApi();
    }

    /**
     * Renders UI
     *
     * @return model and view for UI
     */
    @GetMapping
    @ResponseBody
    public Resource getUi() {
        return new ClassPathResource("index.html", OpenApiController.class);
    }

}
