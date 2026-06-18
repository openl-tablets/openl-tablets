package org.openl.rules.spring.openapi.app140;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

/**
 * The {@code Accept} and {@code Authorization} header parameters are reserved by the OpenAPI specification and must be
 * dropped from the generated document, while ordinary header parameters such as {@code X-Custom-Header} are kept. This
 * holds both for headers bound through {@code @RequestHeader} and for headers declared through a swagger
 * {@code @Parameter} annotation.
 */
@RestController
public class HeaderController {

    @GetMapping("/headers")
    public ResponseEntity<Void> headers(
            @RequestHeader(name = "Accept") String accept,
            @RequestHeader(name = "Authorization") String authorization,
            @RequestHeader(name = "X-Custom-Header") String custom) {
        return ResponseEntity.ok().build();
    }

    @GetMapping("/annotated-headers")
    @Parameter(in = ParameterIn.HEADER, name = "Authorization", description = "Reserved header declared via annotation")
    @Parameter(in = ParameterIn.HEADER, name = "X-Trace-Id", description = "Custom header declared via annotation")
    public ResponseEntity<Void> annotatedHeaders() {
        return ResponseEntity.ok().build();
    }
}
