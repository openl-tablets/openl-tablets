package org.openl.rules.spring.openapi.app030;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@Tag(name = "greetings")
@RequestMapping("/greetings")
@ApiResponses({
        @ApiResponse(responseCode = "404", description = "Not Found", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDto.class))),
        @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorDto.class))) })
public class HelloController {

    @Operation(summary = "Default say hello", description = "Say hello response entity.", operationId = "sayHelloDefault", responses = {
            @ApiResponse(description = "Successful Operation", responseCode = "200", content = @Content(mediaType = "application/json", schema = @Schema(oneOf = {
                    String.class,
                    Integer.class }), examples = { @ExampleObject(name = "The String example", value = "Foo-bar"),
                            @ExampleObject(name = "The Integer example", value = "4221") })) })
    @Tag(name = "default")
    @GetMapping(value = "/hello")
    @Deprecated
    ResponseEntity<Void> sayHello() {
        return null;
    }

    @Operation(summary = "Say Hello")
    @RequestBody(description = "Name", required = true, content = @Content(schema = @Schema(implementation = String.class)))
    @ApiResponse(description = "OK", responseCode = "200")
    @PostMapping("/sayHello")
    public void sayHello(String name) {

    }

}
