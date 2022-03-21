package org.openl.rules.spring.openapi.app001;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

@RestController
public class HelloController {

    @Operation(summary = "Say hello", description = "Say hello response entity.", responses = {
            @ApiResponse(description = "Successful Operation", responseCode = "200", content = @Content(mediaType = "application/json", schema = @Schema(oneOf = {
                    String.class,
                    Integer.class }), examples = { @ExampleObject(name = "The String example", value = "Foo-bar"),
                            @ExampleObject(name = "The Integer example", value = "4221") })),
            @ApiResponse(responseCode = "404", description = "Not Found"),
            @ApiResponse(responseCode = "400", description = "Bad Request") })
    @GetMapping(value = "/hello")
    ResponseEntity<Void> sayHello() {
        return null;
    }

}
