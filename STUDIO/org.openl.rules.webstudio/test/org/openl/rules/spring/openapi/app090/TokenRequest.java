package org.openl.rules.spring.openapi.app090;

import jakarta.validation.constraints.NotBlank;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Request model for creating a token.
 */
@Schema(description = "Request to create a new token")
public record TokenRequest(
        @Schema(description = "Token name", example = "My API Token")
        @NotBlank
        String name
) {
}
