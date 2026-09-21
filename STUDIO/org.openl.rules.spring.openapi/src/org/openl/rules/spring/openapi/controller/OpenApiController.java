package org.openl.rules.spring.openapi.controller;

import java.net.URI;
import jakarta.servlet.http.HttpServletRequest;

import io.swagger.v3.oas.annotations.Hidden;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    /** Where the documentation is drawn, as an address within the application. */
    private static final String UI_PATH = "/api-docs";

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
     * Points a reader at the screen the documentation is drawn on.
     *
     * <p>The documentation used to be a page of its own, served from here. It is a screen of the application
     * now, so this address answers with where that screen is, and a link kept to it still leads there.
     *
     * @param request the request, read for the path the application is served under
     * @return the address of the screen
     */
    @GetMapping("/api-docs")
    public ResponseEntity<Void> getUi(HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(request.getContextPath() + UI_PATH))
                .build();
    }

}
