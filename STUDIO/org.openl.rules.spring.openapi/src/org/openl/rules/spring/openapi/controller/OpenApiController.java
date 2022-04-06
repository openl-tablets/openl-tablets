package org.openl.rules.spring.openapi.controller;

import org.openl.rules.spring.openapi.service.OpenApiServiceImpl;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;

import com.fasterxml.jackson.core.JsonProcessingException;

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
     * @throws JsonProcessingException in case of errors while generation
     */
    @GetMapping(value = "/openapi.json", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public String openApi() throws JsonProcessingException {
        openApiService.build();
        return openApiService.getOpenApiAsJson();
    }

    /**
     * Renders UI
     *
     * @return model and view for UI
     */
    @GetMapping
    public ModelAndView getUi() {
        var view = new ModelAndView();
        view.setViewName("api-ui");
        return view;
    }

}
