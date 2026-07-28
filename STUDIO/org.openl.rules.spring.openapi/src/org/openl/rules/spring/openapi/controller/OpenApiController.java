package org.openl.rules.spring.openapi.controller;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import org.openl.rules.spring.openapi.service.OpenApiSpringMvcReaderImpl;

/**
 * OpenAPI Controller
 */
@Controller
@Hidden
@RequiredArgsConstructor
public class OpenApiController {

    private final OpenApiSpringMvcReaderImpl openApiSpringMvcReader;
    private volatile String openApi;

    /**
     * Gets generated OpenAPI schema as JSON string
     *
     * @return json string
     */
    @GetMapping(value = "/openapi.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String openApi() {
        if (openApi == null) {
            synchronized (this) {
                if (openApi == null) {
                    openApi = openApiSpringMvcReader.read();
                }
            }
        }
        return openApi;
    }

    /**
     * Renders UI
     *
     * @return model and view for UI
     */
    @GetMapping(value = "/api-docs", produces = MediaType.TEXT_HTML_VALUE)
    @ResponseBody
    public Resource getUi() {
        return new ClassPathResource("index.html", OpenApiController.class);
    }

}
