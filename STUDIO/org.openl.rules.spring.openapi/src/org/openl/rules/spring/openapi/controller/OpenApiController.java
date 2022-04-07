package org.openl.rules.spring.openapi.controller;

import org.openl.rules.spring.openapi.service.OpenApiService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.web.servlet.ModelAndView;

@RestController
@RequestMapping("/api-docs")
public class OpenApiController {

    private final OpenApiService openApiService;

    public OpenApiController(OpenApiService openApiService) {
        this.openApiService = openApiService;
    }

    @GetMapping(value = "/openapi.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public String openApi() throws JsonProcessingException {
        openApiService.build();
        return openApiService.getOpenApiAsJson();
    }

    @GetMapping
    public ModelAndView getUi() {
        var view = new ModelAndView();
        view.setViewName("api-ui");
        return view;
    }

}
